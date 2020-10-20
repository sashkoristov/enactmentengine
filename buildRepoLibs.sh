#!/bin/bash

# Script to pull the latest version of all repos defined in the 'repoDependencies/repos.txt' file, build them, and copy their .jar file into the libs/ folder (contains the link to the repo without the https:// prefix, as well as the name of the project (has to match the folder created upon cloning the repository))


# save the current dir
startDir=$(pwd)
mkdir -p libs
echo "Start dir: $startDir"


user=$(whoami)
echo $user

input="./repoDependencies/repos.txt"

# each line describes a repository. For each one, we create a temporary folder, build it there, and then copy the jar files from the build folder to the libs folder of our project.
cat $input | while read line 
do
   echo "processing line: $line"
   wordarray=($line)
   repoPath=${wordarray[0]}
   pathToBuild=${wordarray[1]}
   # make a temporary directory
   mkdir -p tmp
   cd ./tmp
   echo "Cloning repository $repoPath"
   # check the user; use https if user is root (we are on the runner) and ssh if we are on a local machine
   if [ "$user" == "root" ]; then
   		# get the URL and USER used by the runner and use it to clone the repository
   		BASE_URL=`echo $CI_REPOSITORY_URL | sed "s;\/*$CI_PROJECT_PATH.*;;"`
   		REPO_URL="$BASE_URL$repoPath"
   else
   	    BASE_URL="git@goedis.dps.uibk.ac.at:"
   		REPO_URL="$BASE_URL$repoPath"
   fi
   
   git clone $REPO_URL
   cd ./$pathToBuild
   echo "Building repository $folderName"
   ./gradlew --console=plain build </dev/null
   #copy the .jars from the lib folder in the build dir
   cp ./build/libs/*.jar $startDir/libs/
   
   # remove the temporary directory
   cd $startDir
   rm -r tmp/
done

# delete the shadow jar from the libs folder (if created)
rm -f libs/shadow.jar
