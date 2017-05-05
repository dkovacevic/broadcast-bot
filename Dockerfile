FROM wire/bots.runtime:latest

COPY target/channel.jar      /opt/channel/channel.jar
COPY certs/keystore.jks        /opt/channel/keystore.jks

WORKDIR /opt/channel
