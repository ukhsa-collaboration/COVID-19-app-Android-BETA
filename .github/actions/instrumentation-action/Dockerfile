FROM debian:testing

# Base setup with Open JDK

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && apt-get upgrade -y

RUN apt-get -y install --no-install-recommends gnupg man less curl wget unzip git openjdk-13-jdk openjdk-13-source

RUN wget --quiet https://github.com/Flank/flank/releases/download/v20.06.0/flank.jar -O ./flank.jar

RUN export FLANK_HOME=./

# Add action entry point

ADD entrypoint.sh /entrypoint.sh

RUN chmod 755 /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
