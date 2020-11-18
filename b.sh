#!/bin/bash



counter=1
while [ $counter -le 100000 ]
do
   echo  "Message " $counter | kafkacat -P -b localhost:9092 -t my-topic1 
   ((counter++))
done
