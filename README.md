# Swarm Server Monitor

Swarm is an application aggregating hardware data from a _swarm_ of servers.

## Components

* A web server built with [Ktor], written in [Kotlin]
* Dragonfly, a script aggregating and sending hardware data to Swarm, written in bash
* A client application displaying data retrieved from the web server, build with [React] and [D3.js]

## Goals

* Using [React] in an actual project
* Learning [D3.js]
* Playing with early versions of [Ktor]
* Managing to use [D3.js] with [React]
* Replace [NewRelic Servers]

## Usage

You need to host your own instance of Swarm.

### Swarm

#### Build
````bash
git clone https://github.com/Ribesg/Swarm.git
cd Swarm
mvn
````

#### Run
Get the resulting jar file (`target/Swarm.jar`), move it where you want and run it
````bash
cd ..
mv Swarm/target/Swarm.jar .
java -jar Swarm.jar --key $KEY --slack-hook $SLACK_HOOK
````

All available arguments:
````
         -d, --debug - Enable debug output
       -v, --verbose - Enable verbose output
--dev, --development - Enable development mode
              --host - Local host to bind to (default: 0.0.0.0)
              --port - Local port to bind to (default: 80)
           -k, --key - Key accepted from Dragonfly clients
    -s, --slack-hook - Slack WebHook used to send alerts (default: alerts disabled)
````

### Dragonfly

#### Install
````bash
wget https://raw.githubusercontent.com/Ribesg/Swarm/master/dragonfly.sh
chmod +x dragonfly.sh
./dragonfly.sh --install --key $KEY --api $API_URL
rm dragonfly.sh
````

All available Dragonfly arguments:
````
           -a, --api - Configure the Swarm instance URL
          -h, --help - Show this help
       -i, --install - Install the Dragonfly Client
           -k, --key - Configure the key used to send data to the Swarm instance
         -l, --login - Configure the HTTP user used to send data to the Swarm instance
-o, --run-one-minute - Run the Dragonfly Client for 1 minute
      -p, --password - Configure the HTTP password used to send data to the Swarm instance
  -r, --run-infinite - Run the Dragonfly Client forever (i.e. until killed)
     -u, --uninstall - Uninstall the Dragonfly Client

Options -i, -o and -r requires options -a and -k.
````

Dragonfly installs itself in `/opt/var/dragonfly` and as a cron task,
starting every minute and running for one minute.

#### Uninstall
````bash
/opt/var/dragonfly/dragonfly.sh --uninstall
````



[D3.js]: https://d3js.org/
[Kotlin]: https://kotlinlang.org/
[Ktor]: http://ktor.io/
[NewRelic Servers]: https://docs.newrelic.com/docs/servers
[React]: https://reactjs.org/
