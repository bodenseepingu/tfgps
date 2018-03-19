#!/bin/bash
##
# Copyright Airbus DS GmbH (c) 2016
#  
# For configuration item number / versioning information / 
# history of file-modifications see CM documents and GIT-repository
##
# Usage documentation here

# BUILDROOT is the source of the rpmbuild build prozess. 
# Calling rpm-build Tools creates always the following structure unter BUILDROOT:
# BUILD,RPMS,SOURCES,SPECS,SRPMS. 
# PRMS is the only one which is needed. Here you find the generated rpm file.
# Files in BUILDROOT can be specified in %files section for packaging.
# [JM] xx.yy.2015, Version 1.01, initial
# [TS] 07.03.2016, Version 1.02, new structured
# [TS] 10.10.2016, Version 1.03, customized to wrlagez
# [TS] 17.10.2016, Version 1.04, only if mkdir is executed, debug message.

##########################################
# exit, if return code is not 0
# origin: generated by NetBeans
##########################################

function checkReturnCode () {

    rc=$?
    if [ $rc != 0 ]
    then
        echo "ERROR abort, returncode is $rc"
        exit $rc
    fi
}

##########################################
# helper: create dir, if it doesn't exist
##########################################
function createDir () {
typeset -i rc=0
 if [[ ! -d $1 ]] 
 then 
   echo "createDir: $1"
   mkdir -p $1
   rc=$?
 fi
 return ${rc}
}
##########################################
# clean
##########################################
function clean () {
 typeset -i rc=0
 echo "DEBUG clean:"
 # Cleanup
 if [[ -n ${RPM_MAIN_DIR} && -d ${RPM_MAIN_DIR} ]]
 then 
   echo "DEBUG Delete ${RPM_MAIN_DIR}"
   # will be removed only, if variable is not null !!! (perhaps -force)
   rm -r ${RPM_MAIN_DIR}
   rc=$?
 fi
 return ${rc}
}
##########################################
# Ensure proper rpm build environment
# set the topdir 
# in old fedora buildroot param is ignored
# not needed any more in 11/2015
##########################################
function setTopdir () {
 typeset -i rc=0
 MYTOPDIR=${1:-${RPM_MAIN_DIR}}
 echo "INFO  setTopdir to ${MYTOPDIR}"
 if [ -f ${HOME}/.rpmmacros ]
 then
   createDir /tmp/${USER}
   rc=${rc}+$?
   # cut old entry
   grep -v "%_topdir " ${HOME}/.rpmmacros > /tmp/${USER}/rpmmacros_temp.txt
   mv /tmp/${USER}/rpmmacros_temp.txt ${HOME}/.rpmmacros
   rc=${rc}+$?
 fi
 # set new entry
 echo "%_topdir ${MYTOPDIR}"  >> ${HOME}/.rpmmacros
 rc=${rc}+$?
 return ${rc}
}
############################################
# Extract tar distribution to packaging dir
############################################
function extractDistribution () {
 typeset -i rc=0
 echo "DEBUG extractDistribution:"
 #extract tar content to RPM_BUILDROOT
 # without slash, because EXTRACT_DIR starts with /opt/..
 createDir ${RPM_BUILDROOT}${EXTRACT_DIR}
 rc=${rc}+$?
 echo "DEBUG tar -xvzf ${DISTRIBUTION_TAR} ${RPM_BUILDROOT}${EXTRACT_DIR}/"
 tar -xvzf ${DISTRIBUTION_TAR} -C ${RPM_BUILDROOT}${EXTRACT_DIR}/
 rc=${rc}+$?
 return ${rc}
}
##########################################
# prepare
##########################################
function prepare () {
 typeset -i rc=0
 echo "DEBUG prepare:"
 createDir ${RPM_BUILDROOT}
 rc=${rc}+$?
 setTopdir ${RPM_MAIN_DIR}
 rc=${rc}+$?
 extractDistribution
 rc=${rc}+$?
 return ${rc}
}

##########################################
# build, generate rpm
##########################################
function build () {
 echo "DEBUG build:"
 rpmbuild -v --buildroot ${RPM_BUILDROOT} -bb ${SPEC_FILE} > ${RPM_LOG_FILE}
 return $?
}

##########################################
# deploy
##########################################
function deploy () {
 echo "DEBUG deploy:"
 typeset -i rc=0
 cat ${RPM_LOG_FILE}
 RPM_FULL_NAME=$(cat ${RPM_LOG_FILE} | grep '\.rpm' | tail -1 |awk -F: '{ print $2 }')
 rc=${rc}+$?
 echo "DEBUG ${RPM_PATH}"
 RPM_NAME=$(basename ${RPM_FULL_NAME})
 rc=${rc}+$?
 echo "DEBUG ${RPM_NAME}"
 mv ${RPM_FULL_NAME} ${RPM_RELEASE_DIR}/
 rc=${rc}+$?
 echo "DEBUG RPM: ${RPM_RELEASE_DIR}/${RPM_NAME}"
 return ${rc}
}

##########################################
# main
##########################################
# 0. Gradle Standard directories

GRADLE_BUILD_DIR=$(pwd)/build
# 1. INPUT
SPEC_FILE=./src/rpm/spec/installer.spec
# PROJECT_VERSION given by gradle job
export PROJECT_VERSION=${PROJECT_VERSION:-0.1.1}
# PROJECT_NAME given by gradle job
export PROJECT_NAME=${PROJECT_NAME:-contacteditor}
DISTRIBUTION_TAR=${GRADLE_BUILD_DIR}/distributions/${PROJECT_NAME}.tgz
# 2. work and generate
EXTRACT_DIR=/opt/airbusds/nbe
RPM_MAIN_DIR=${GRADLE_BUILD_DIR}/tmp/rpm
RPM_BUILDROOT=${RPM_MAIN_DIR}/BUILDROOT
RPM_LOG_FILE=${RPM_MAIN_DIR}/rpmbuild.log
# 3. result destination
RPM_RELEASE_DIR=${GRADLE_BUILD_DIR}/distributions
# run
clean
checkReturnCode
prepare
checkReturnCode
build
checkReturnCode
deploy
echo "INFO result is $?"