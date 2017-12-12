#!/bin/env python
import re
import os
import sys

#Script should be executed by Gradle build routine
#from main app module inside of an IDEA project structure 
#i.e. from android/apps/OptalertEagle/optalertEagle
#
#Installation for Windows:
#Install python from http://www.python.org/download/releases/
#Please select one of 2.7.x versions for your platform
#Please do not select 3.x version as script will not be compatible
#Ensure you select "Add Python to PATH" or add C:\Python27\ to PATH manually

#Configuration variables
manifest_filepath = "./src/main/AndroidManifest.xml"
version_filepath = "../Version.txt"

#get app version from version.txt
version_file = open(version_filepath, "r")
version_name = version_file.read().split()[0]
version_file.close()

revision_to_put_s = "00000"

#now read and parse manifest
from xml.dom.minidom import parse
dom1 = parse(manifest_filepath)

print ( "Updating Manifest..." )
dom1.documentElement.setAttribute("android:versionName", version_name + revision_to_put_s )
dom1.documentElement.setAttribute("android:versionCode",  revision_to_put_s )
manifest_file = open(manifest_filepath, "w")
manifest_file.write( dom1.toxml(encoding="utf-8") )
manifest_file.close()

print ("BUILD: " + dom1.documentElement.getAttribute("android:versionName") )
print ( "Version Reset Completed" )
