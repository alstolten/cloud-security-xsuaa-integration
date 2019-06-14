#! /usr/bin/env bash

#functions
usage() { echo -e >&2 "Usage: ./xs-logcollector.sh <app-name> <approuter-name> [output-file]\nIf no output file is specified $HOME/logcollection.zip will be used."; exit 0; }

checkappname() {
	xs app "$1" --guid &>/dev/null || { echo -e >&2 "\nApp/Approuter \"$1\" not found, did you target the correct space?"; exit 1; }
}

#Variables
appname="$1"
approutername="$2"
logszip="$HOME/logcollection.zip"

if [[ -n $3 ]]
then
	logszip="$3"
fi

#Check number of args
if [[ $# -lt 2 || $# -gt 3 ]]
then
	usage
	exit 1
fi

while getopts "h" arg; do
    case "$arg" in
        h | *)
           	 usage
           	 ;;
    esac
done
shift $((OPTIND-1))

#Checking if xs-cli is installed
hash xs 2>/dev/null || { echo >&2 "xs command line client not found, please install xs cli first (see \"Tip\" at https://help.sap.com/viewer/4505d0bdaf4948449b7f7379d24d0f0d/2.0.03/en-US/addd59069e6f444ca6ccc064d131feec.html."; exit 1; }

#login to the correct API endpoint
echo -e "\nLogging in...\n"
xs login || { echo -e >&2 "\nScript aborted due to failed login. Please check your credentials and try again."; exit 1; }

echo -e "\nSuccessfully logged in, will continue...\n"

checkappname "$appname"
checkappname "$approutername"

printf "\nThis will restart your application \e[36m\e[1m%s\e[0m and your application router \e[36m\e[1m%s\e[0m twice. \nAre you sure (y/n)?" "$appname" "$approutername"
read -rs -n 1 -p "" answer
if [ "$answer" != "${answer#[Yy]}" ]
then
    true
else
    echo -e "\nAborted. Please make sure that it is safe to restart your application before executing this script again."
	exit 0
fi

#Set the log-levels, enviroment variables, restage and restart the apps
echo -e "\nSetting log levels...\n"
xs set-logging-level "$approutername" "*" debug
xs set-logging-level "$appname" "*" debug
xs set-env "$approutername" REQUEST_TRACE true
xs set-env "$appname" REQUEST_TRACE true

echo -e "\nRestage and restart the app and the approuter...\n"
xs restage "$approutername"
xs restart "$approutername"
xs restage "$appname"
xs restart "$appname"

#Creating, collecting and compressing the logs
echo -e "\n\e[36m\e[1mNow please repeat your scenario (e.g. try to login to your app or similar)...\e[0m\n"
read -rp "When you are done please press ENTER to collect the logs:"

echo -e "\nCollecting the logs..."

#Need to use --all in XS A environment, --recent is to short
{ echo -e "Approuter logs:\n\n"; xs logs "$approutername" --all; echo -e "\n\nApp logs:\n\n"; xs logs "$appname" --all; } | zip -q "$logszip" -

#Unsetting log-levels, env variables and restarting apps
echo -e "\nRestoring log levels...\n"
xs unset-logging-level "$approutername" "*"
xs unset-logging-level "$appname" "*"
xs unset-env "$approutername" REQUEST_TRACE
xs unset-env "$appname" REQUEST_TRACE

echo -e "\nRestart the app and the approuter...\n"
xs restage "$approutername"
xs restart "$approutername"
xs restage "$appname"
xs restart "$appname"

#End
echo -e "\n\e[32m\e[1mAll done.\e[0m Your file is here:" && readlink -f "$logszip"
