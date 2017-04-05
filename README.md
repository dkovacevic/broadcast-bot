# Broadcast bot
 This Bot can be used as Broadcast Channel for broad audience. In order to subscribe to a channel user should use the link:
  https://wire.com/b/[channel_name]. There is no limitation as in number of subscribers or number of messages. 
  Once the message is broadcast it will take couple of seconds to be delivered to all subscribers. 
  Message can be: text, web page, picture (a url to jpg|png|gif), youtube link, soundcloud link ... 
  All messages are stored in broadcast.db SQLite DB on the server. Incoming messages from the subscribers are also stored in broadcast.db. 
  
## Channels vs. Groups
 - Unlimited number of subscribers/participants
 - Able to see the previous messages after joining the channel
 - Nobody except the Channel Admin can see the list of subscribers/participants
 - Only Channel Admin can see the messages posted by subscribers
 - Channel can be advertised and anybody can subscribe to it
 - Only Channel Admin controls who can join the channel and what is posted
 - Subscribers posting in a Channel is faster than posting in a big Group conversation

## Build the project
 ```
 mvn -Plinux package
 ```
 
*windows* and *darwin* are also supported. Running *gen_cert.sh* will generate self signed certificate and keystore file
 (stored in *certs* folder). Modify the script before the run in order to better reflect your company's name/country...

## Register as Wire Bot Developer 
This is done using DevBot. Go to https://wire.com/b/devbot and log in with your Wire credentials 
- "DevBot" is a bot to set up your developer account and create your own bots. 
More info on how to register and setup bots can be found [here](https://github.com/wireapp/wire-bot-java)

## Create new bot
Again through DevBot: Create your bot (in this case it will be a bot to serve as a channel). 
Pick up some unique/catchy name for the bot. When asked to provide the public key use the one from *./certs/pubkey.pem*

## Deployment
Deploy:
```
broadcast.jar
broadcast.yaml
keystore.jks
```
files to your server. Notice that you will need a Public IP to serve as endpoint that will be called by the Wire Backend

## Run the Service
```
java -jar broadcast.jar server broadcast.yaml
```

## Setup Admin Conversation
In order to receive all the incoming messages from the subscribers you can select an existing conversation with the broadcast bot
 and let the service channel all the messages there. Update `admin` param in `broadcast.yaml` with the BotId.
BotId is printed as first message when subscribing (also visible in logs)

## How to Broadcast messages
Go to: http://localhost:8062/assets/message.html and post your first message
Alternatively: posting into Admin Conversation would broadcast the message to all subscribers

## List all Incoming Messages and Broadcasts
 ```
 curl -XPOST http://localhost:8061/tasks/messages
 curl -XPOST http://localhost:8061/tasks/broadcasts
 ```