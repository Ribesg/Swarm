#!/usr/bin/env bash
# Dragonfly - Swarm Client

declare SWARM_URL=""
declare SWARM_KEY=""
declare SWARM_HTTP_USER=""
declare SWARM_HTTP_PASS=""
readonly VERSION="0.0.1"
readonly SLEEP_TIME=1

readonly INSTALLED_SCRIPT_DIR="/opt/var/dragonfly"
readonly INSTALLED_SCRIPT_PATH="$INSTALLED_SCRIPT_DIR/dragonfly.sh"
readonly INSTALLED_CRON_PATH="/etc/cron.d/dragonfly"

declare -a REQUIRED_COMMANDS=( "cat" "curl" "date" "grep" "sed" "xargs" )

declare NOW JSON_PAYLOAD NET_DATA DISK_DATA
declare -a CPU_DATA RAM_DATA

# Checks if a command is installed
# $1 = String - The command to check
isCommandInstalled() {
	return $(hash "$1" 2>/dev/null)
}

checkCommands() {
	for cmd in ${REQUIRED_COMMANDS[@]}; do
		if ! isCommandInstalled ${cmd}; then
			echo "Command not found: $cmd"
			exit 1
		fi
	done
}

# Sets the CPU_DATA array to up to date values
setCpuData() {
	CPU_DATA=(`sed -n 's/^cpu\s//p' /proc/stat | xargs`)
}

# Sets the RAM_DATA array to up to date values
setRamData() {
	local MEM_INFO=$(cat /proc/meminfo)
	local -i RAM_TOTAL=$(echo "$MEM_INFO" | grep "MemTotal" | grep -o '[0-9]*')
	local -i RAM_FREE=$(echo "$MEM_INFO" | grep "MemAvailable" | grep -o '[0-9]*')
	local -i RAM_SWAP_TOTAL=$(echo "$MEM_INFO" | grep "SwapTotal" | grep -o '[0-9]*')
	local -i RAM_SWAP_FREE=$(echo "$MEM_INFO" | grep "SwapFree" | grep -o '[0-9]*')

	# MemAvailable is not available on all systems, we fall back to MemFree if needed
	if [[ ${RAM_FREE} -eq 0 ]]; then
		RAM_FREE=$(echo "$MEM_INFO" | grep "MemFree" | grep -o '[0-9]*')
	fi

	RAM_DATA=("$RAM_TOTAL" "$RAM_FREE" "$RAM_SWAP_TOTAL" "$RAM_SWAP_FREE")
}

# Sets the NET_DATA json object to up to date values
setNetData() {
	local TMP_NET_DATA="{";
	local -i linesIgnored=0
	while read line; do
		if [[ ${linesIgnored} < 2 ]]; then
			linesIgnored=$((linesIgnored + 1));
		else
			IFS=':' read -ra PARTS <<< "$line"
			local INTERFACE=${PARTS[0]}
			local DATA=$(echo "${PARTS[1]}" | xargs)
			TMP_NET_DATA="$TMP_NET_DATA\"$INTERFACE\": \"$DATA\","
		fi
	done < /proc/net/dev
	NET_DATA="${TMP_NET_DATA::-1}}"
}

# Sets the DISK_DATA json object to up to date values
setDiskData() {
	local TMP_DISK_DATA="{"
	local DF_RESULT=$(df -T | sed -n 's/^\/dev\///p')
	TMP_DISK_DATA="$TMP_DISK_DATA\"df\":["
	while read line; do
		local DATA=$(echo "$line" | xargs)
		TMP_DISK_DATA="$TMP_DISK_DATA\"$DATA\","
	done <<< "$DF_RESULT"
	TMP_DISK_DATA="${TMP_DISK_DATA::-1}],\"diskstats\":["
	while read line; do
		local DATA=$(echo "$line" | xargs)
		TMP_DISK_DATA="$TMP_DISK_DATA\"$DATA\","
	done < /proc/diskstats
	DISK_DATA="${TMP_DISK_DATA::-1}]}"
}

setNow() {
	NOW=$(date +%s%3N)
}

setPayload() {
	local HOST=$(hostname)
	# Wrap all our data in a JSON object
	read -r -d '' JSON_PAYLOAD <<-EndOfJson
		{
			"key": "${SWARM_KEY}",
			"host": "${HOST}",
			"date": "${NOW}",
			"cpu": "${CPU_DATA[@]}",
			"ram": "${RAM_DATA[@]}",
			"net": ${NET_DATA},
			"disk": ${DISK_DATA}
		}
	EndOfJson
	# Replace newlines with spaces
	JSON_PAYLOAD=$(echo ${JSON_PAYLOAD} | sed -e 's/[\n\r ]+/ /g')
	# Remove spaces after opening brackets
	JSON_PAYLOAD=$(echo ${JSON_PAYLOAD} | sed -e 's/{ "/{"/g')
	# Remove spaces before closing brackets
	JSON_PAYLOAD=$(echo ${JSON_PAYLOAD} | sed -e 's/" }/"}/g')
	# Remove spaces after colons
	JSON_PAYLOAD=$(echo ${JSON_PAYLOAD} | sed -e 's/": "/":"/g')
	# Remove spaces after commas
	JSON_PAYLOAD=$(echo ${JSON_PAYLOAD} | sed -e 's/", "/","/g')
}

setAllData() {
	setNow
	setCpuData
	setRamData
	setNetData
	setDiskData
	setPayload
}

# Sends the provided JSON payload to the provided URL using an HTTP POST request.
# $1 = URL: String
# $2 = JSON: String
sendData() {
	if [[ -z "$SWARM_HTTP_USER" ]]; then
		curl                                                \
			--fail --silent --show-error                    \
			--header "Content-Type:application/json"        \
			--data "$2"                                     \
			"$1"
	else
		curl                                                \
			--fail --silent --show-error                    \
			--user "$SWARM_HTTP_USER:$SWARM_HTTP_PASS"      \
			--header "Content-Type:application/json"        \
			--data "$2"                                     \
			"$1"
	fi
}

runOnce() {
	setAllData
	echo "$NOW: sending data"
	sendData "$SWARM_URL" "$JSON_PAYLOAD"
}

loopForAMinute() {
	echo "Running every 5 seconds for 1 minute"
	for i in $(seq 0 5 55); do
		runOnce &
		sleep 5
	done
}

loopForever() {
	echo "Running every 5 seconds forever"
	while true; do
		runOnce &
		sleep 5
	done
}

install() {
	echo "Installing Dragonfly"
	mkdir -p "$INSTALLED_SCRIPT_DIR"
	cat "$0" > "$INSTALLED_SCRIPT_PATH"
	chmod +x "$INSTALLED_SCRIPT_PATH"
	local OPTIONS="-o -a '$SWARM_URL' -k '$SWARM_KEY'"
	if [[ -z "$SWARM_HTTP_USER" ]]; then
		OPTIONS="$OPTIONS -l '$SWARM_HTTP_USER'"
	fi
	if [[ -z "$SWARM_HTTP_PASS" ]]; then
		OPTIONS="$OPTIONS -p '$SWARM_HTTP_PASS'"
	fi
	echo "* * * * * root $INSTALLED_SCRIPT_PATH $OPTIONS >> /var/log/dragonfly.log 2>&1" > "$INSTALLED_CRON_PATH"
	echo "Done installing Dragonfly"
}

uninstall() {
	echo "Uninstalling Dragonfly..."
	rm -f "$INSTALLED_CRON_PATH"
	rm -f "$INSTALLED_SCRIPT_PATH"
	rmdir "$INSTALLED_SCRIPT_DIR"
	echo "Done uninstalling Dragonfly!"
}

printVersion() {
	echo "Dragonfly - Swarm Client - v$VERSION"
}

printHelp() {
	echo
	printVersion
	echo "           -a, --api - Configure the Swarm instance URL"
	echo "          -h, --help - Show this help"
	echo "       -i, --install - Install the Dragonfly Client"
	echo "           -k, --key - Configure the key used to send data to the Swarm instance"
	echo "         -l, --login - Configure the HTTP user used to send data to the Swarm instance"
	echo "-o, --run-one-minute - Run the Dragonfly Client for 1 minute"
	echo "      -p, --password - Configure the HTTP password used to send data to the Swarm instance"
	echo "  -r, --run-infinite - Run the Dragonfly Client forever (i.e. until killed)"
	echo "     -u, --uninstall - Uninstall the Dragonfly Client"
	echo
	echo "Options -i, -o and -r requires options -a and -k."
}

checkApiUrlAndKey() {
	if [[ -z "$SWARM_URL" ]]; then
		echo "Error: Missing API URL parameter."
		exit 1
	fi
	if [[ -z "$SWARM_KEY" ]]; then
		echo "Error: Missing KEY parameter."
		exit 1
	fi
}

main() {

	# We will need to copy this script somewhere, for this we need the name to be right
	if [[ $0 != *dragonfly.sh ]]; then
		echo "Error: it looks like this script is not run from a file on the filesystem"
		echo "       Found path: \"$0\" (should end with 'dragonfly.sh')"
		exit 1
	fi

	# We need root to make sure we can deal with cron and copying this script where we want to
	if [[ $EUID > 0 ]]; then
		printVersion
		echo "Error: please run dragonfly.sh as root"
		exit 1
	fi

	# Check that the commands we use exists in this environment
	checkCommands

	# Check that we have the right version of getopt
	getopt --test > /dev/null
	if [[ $? -ne 4 ]]; then
		printVersion
		echo "Error: environment does not support enhanced getopt"
		exit 1
	fi

	# Parse options using getopt
	local OPTIONS="a:hik:l:op:ru"
	local LONG_OPTIONS="api:,help,install,key:,login:,run-one-minute,password:,run-infinite,uninstall"
	local PARSED_ARGS=$(getopt -n "$0" -o "${OPTIONS}" -l "${LONG_OPTIONS}" -- "$@")
	if [[ $? -ne 0 ]]; then
		# error, getopt already complained to stdout so we don't have to print anything
		exit 1
	fi
	eval set -- "${PARSED_ARGS}"

	# If the first argument returned by getopt is '--', then no argument was passed to dragonfly.sh
	if [[ "$1" == "--" ]]; then
		printHelp
		exit 0
	fi

	# Read and put parsed arguments into nice variables
	local ARG_API=""
	local ARG_HELP=false
	local ARG_INSTALL=false
	local ARG_KEY=""
	local ARG_KEY_PROVIDED=false
	local ARG_LOGIN=""
	local ARG_LOGIN_PROVIDED=false
	local ARG_RUN_ONE_MINUTE=false
	local ARG_PASSWORD=""
	local ARG_PASSWORD_PROVIDED=false
	local ARG_RUN_INFINITE=false
	local ARG_UNINSTALL=false

	local ARG_UNIQUE_COUNT=0

	while true; do
		case "$1" in

			-a|--api)
				ARG_API="$2"
				shift 2
				;;

			-h|--help)
				ARG_HELP=true
				shift
				;;

			-i|--install)
				ARG_INSTALL=true
				((ARG_UNIQUE_COUNT++))
				shift
				;;

			-k|--key)
				ARG_KEY="$2"
				ARG_KEY_PROVIDED=true
				shift 2
				;;

			-l|--login)
				ARG_LOGIN="$2"
				ARG_LOGIN_PROVIDED=true
				shift 2
				;;

			-o|--run-one-minute)
				ARG_RUN_ONE_MINUTE=true
				((ARG_UNIQUE_COUNT++))
				shift
				;;

			-p|--password)
				ARG_PASSWORD="$2"
				ARG_PASSWORD_PROVIDED=true
				shift 2
				;;

			-r|--run-infinite)
				ARG_RUN_INFINITE=true
				((ARG_UNIQUE_COUNT++))
				shift
				;;

			-u|--uninstall)
				ARG_UNINSTALL=true
				((ARG_UNIQUE_COUNT++))
				shift
				;;

			--)
				# End of the arguments list
				shift
				break
				;;

			*)
				# We should never reach this
				echo "Programming error"
				exit 3
				;;

		esac
	done

	# Prevents running things like install and uninstall at the same time
	if [[ ${ARG_UNIQUE_COUNT} -gt 1 ]]; then
		printVersion
		echo "Error: Only one of -i, -o, -r and -u allowed"
		exit 1
	fi

	# If the help argument is provided anywhere, print help and exit
	if [[ ${ARG_HELP} == true ]]; then
		printHelp
		exit 0
	fi

	# Read configuration arguments
	if [[ -n "${ARG_API}" ]]; then
		SWARM_URL=${ARG_API}
	fi
	if [[ ${ARG_KEY_PROVIDED} == true ]]; then
		SWARM_KEY=${ARG_KEY}
	fi
	if [[ ${ARG_LOGIN_PROVIDED} == true ]]; then
		SWARM_HTTP_USER=${ARG_LOGIN}
	fi
	if [[ ${ARG_PASSWORD_PROVIDED} == true ]]; then
		SWARM_HTTP_PASS=${ARG_PASSWORD}
	fi

	# Finally, run whatever we are supposed to run

	if [[ ${ARG_INSTALL} == true ]]; then
		checkApiUrlAndKey
		install
		exit 0
	fi

	if [[ ${ARG_UNINSTALL} == true ]]; then
		uninstall
		exit 0
	fi

	if [[ ${ARG_RUN_ONE_MINUTE} == true ]]; then
		checkApiUrlAndKey
		loopForAMinute
		exit 0
	fi

	if [[ ${ARG_RUN_INFINITE} == true ]]; then
		checkApiUrlAndKey
		loopForever
		exit 0
	fi

}

main "$@"
