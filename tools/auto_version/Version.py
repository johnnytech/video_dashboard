#!/bin/env python
import re
import os
import sys
import ResetVersion

#Script should be executed by Gradle build routine
#from main app module inside of an IDEA project structure 
#i.e. from android/apps/OptalertEagle/optalertEagle
#
#Script is comparing "svnversion" output to Manifest version code
#and if svnversion shows this branch had commits - Manifest is updated.
#new versionCode is taken as "svn info -r 'HEAD'" revision + 1;
#and versionName will be set to contents of Version.txt (like of A01.00.) + versionCode
#
#Installation for Windows:
#Install python from http://www.python.org/download/releases/
#Please select one of 2.7.x versions for your platform
#Please do not select 3.x version as script will not be compatible
#Ensure you select "Add Python to PATH" or add C:\Python27\ to PATH manually


#Configuration variables
root_dir = "../."
manifest_filepath = "./src/main/AndroidManifest.xml"
version_filepath = "../Version.txt"

print ( "Auto Versioning started" )

#cleanup 1.6 vs 1.7 inconsistency
os.popen("svn upgrade " + root_dir)
os.popen("svn cleanup " + root_dir)


#get app version from version.txt
version_file = open(version_filepath, "r")
version_name = version_file.read().split()[0]
version_file.close()


#Get current branch revision from svn info
svninfo_string = os.popen("svn info " + root_dir).read()
svninfo_words = svninfo_string.split()
svninfo_revision_n = int( svninfo_words[svninfo_words.index("Revision:") + 1] )
svninfo_revision_s = str( svninfo_revision_n )


#Get current branch revision from svnvrsion
svnversion_string = os.popen("svnversion -n -c " + root_dir).read()
delimiter_index = svnversion_string.find(":")
if delimiter_index == -1 :
	svnverson_revision_s = svnversion_string
else:
	svnverson_revision_s = svnversion_string[delimiter_index + 1 : ]

svnverson_revision_n = int ( re.findall(r'\d+', svnverson_revision_s)[0]	)


#extract repository head
headinfo_string = os.popen("svn info -r HEAD").read()
headinfo_words = headinfo_string.split()
revision_to_put_n = int( headinfo_words[headinfo_words.index("Revision:") + 1] ) + 1
revision_to_put_s = str( revision_to_put_n )


#now read and parse manifest
from xml.dom.minidom import parse
dom1 = parse(manifest_filepath)

print ("Current manifest revision: " + dom1.documentElement.getAttribute("android:versionCode"))
print ("Current svninfo branch revision: " + svninfo_revision_s)
print ("Current svnversion branch revision: " + svnverson_revision_s)
print ("Repository predicted revision: " + revision_to_put_s)

need_to_update = None

if int( dom1.documentElement.getAttribute("android:versionCode") ) < svnverson_revision_n :
	need_to_update = True

if version_name not in dom1.documentElement.getAttribute("android:versionName"):
	need_to_update = True

if need_to_update:
	print ( "Updating Manifest..." )
	dom1.documentElement.setAttribute("android:versionName", version_name + revision_to_put_s )
	dom1.documentElement.setAttribute("android:versionCode",  revision_to_put_s )
	manifest_file = open(manifest_filepath, "w")
	manifest_file.write( dom1.toxml(encoding="utf-8") )
	manifest_file.close()
else:
	print ( "No need to update Manifest" )

print ("BUILD: " + dom1.documentElement.getAttribute("android:versionName") )
print ( "Auto Versioning done" )

