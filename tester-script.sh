#!/bin/bash

COUNTER=0
while :
do
   RESPONSE_PAYLOAD=$(curl -s https://xxxxxx.execute-api.eu-south-1.amazonaws.com/development/ephemeral-storages | jq '.')

   echo $RESPONSE_PAYLOAD
   if [[ $RESPONSE_PAYLOAD =~ 'error' ]]; then
      echo $COUNTER
      break
   fi
   ((COUNTER+=1))
done