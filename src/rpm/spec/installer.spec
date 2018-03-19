##
#
# Copyright Airbus DS GmbH (c) 2015
#  
# For configuration item number / versioning information / 
# history of file-modifications see CM documents and GIT-repository
#
##

Summary: NBE Contact Editor
Name: %{getenv:PROJECT_NAME}
Version:  %{getenv:PROJECT_VERSION}

%define basedir //opt/airbusds/nbe

%define buildnumber %{getenv:BUILD_NUMBER}
%if %{buildnumber}0
Release: %{buildnumber}%{?dist}
%else
Release: 0%{?dist}
%endif

Group: Applications/Airbus DS/NBE
License: Airbus DS, Apache 2.0, LGPL 2.1
Distribution: PhAr-BS NBE
Vendor: Airbus DS GmbH
BuildArch: noarch
#Requires: java

%description
This package contains the graphical contact editor
 PHSE NBE contacteditor for SARah.

%prep

%files
#Permission configuration for shell scripts
#User:rwx=7
#Group:rx=5
#Other:r=4
#%attr(0754, user, group)
"%basedir/*"

#This section creates empty directories (which must be created in %prep)
#%dir

#pre installation script
#%pre

#post installation script
%post
unzip -d %{basedir}/%{getenv:PROJECT_NAME} %{basedir}/%{getenv:PROJECT_NAME}/etc/orekit-data.zip
#pre uninstallation script
%preun
rm -rf %{basedir}/%{getenv:PROJECT_NAME}/orekit-data
#post uninstallation script
#%postun

