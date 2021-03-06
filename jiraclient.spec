
Name: jiraclient
Summary: A command line client for Jira.
BuildArch: noarch
Version: 2.0.0
Release: 1
License: GPL
Vendor:  The Genome Center at Washington University
Packager: %packager
Group: System/Utilities
Source0:   %{name}-%version.tar.gz
BuildRoot: %{_tmppath}/%{name}2-%{version}-%{release}-build
Requires: python, python-fpconst, python-restkit, python-yaml

%description
Jiraclient is a command line utility for Atlassian's Jira Issue Tracker.
It leverages SOAP or XML-RPC, depending upon the API selected in the
configuration.  It allows command line access to a number of Jira tasks
like issue creation and creation of many issues via YAML templates.

%prep
%setup

%build

%install
install -D -m 0755 jiraclient %{buildroot}/usr/bin/jiraclient
install -D -m 0644 jiraclient.py %{buildroot}/usr/share/pyshared/jiraclient/jiraclient.py

%clean
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}

%files
%defattr(-,root,root)
/usr/bin/jiraclient
/usr/share/pyshared/jiraclient

%changelog
* Thu Apr 12 2012 Matthew Callaway <mcallawa@genome.wustl.edu>
  [ 2.0.0-1 ]
- Update for jira 5 REST API

* Wed Jan 4 2012 Matthew Callaway <mcallawa@genome.wustl.edu>
  [ 1.6.10-1 ]
- Add --spent option
- Add --remaining option
- Add --delete option
- Add --worklog option
- Support work log comment on resolution.

* Thu Dec 15 2011 Matthew Callaway <mcallawa@genome.wustl.edu>
  [ 1.6.9-1 ]
- Add --resolve action
- Support resolve upon create.
- Fix addComment.

* Fri Dec  3 2010 Matthew Callaway <mcallawa@genome.wustl.edu>
  [ 1.6.8-1 ]
- ISSOFT-12: Support text names for components and fixVersions

* Mon Nov 15 2010 Matthew Callaway <mcallawa@genome.wustl.edu>
  [ 1.6.7-1 ]
- Added Debian packaging.

* Fri Oct  8 2010 Matthew Callaway <mcallawa@genome.wustl.edu>
  [ 1.5.6-1 ]
- Added RPM packaging.

