# Chatting App for Android (incl. server function)

This a chat app I created for Android OS (client), with server functionality (broker).
The users have the ability to exchange text messages, as well as files of any kind.

## Requirements

Minimum requirements to start the app is a computer to host a local server and two client devices
(real or virtual Android smartphones) to communicate with each other. Maximum tested devices are
three servers on a single computer, two virtual clients and two real android devices simultaneousley. 

## Installation Instructions

Clone the app to a local folder of your choice and do the following to set up the system:

### Server

If you wish to set up multiple brokers, you have to set up a root broker and the number of other
brokers you desire. Execute the following steps to proceed:

#### Root Broker

1. Open the `Broker` folder in your code editor.
2. If not automatically added, add the `LTest.jar` library from the folder `libs` as a dependency to 
   your code.
3. In the `Broker.java` file change the root ip on line 12 to the ip of your system.
4. The `topics.txt` file contains all the chat rooms or conversation topics available for the users of the
   client app to subscribe to. Change its path on line 40 of `Broker.java` to that of your local folder.
5. Run `Broker.java` in your terminal.
6. Enter `4321` as the broker's Port Number when prompted.
7. Enter `y` when asked if you are connecting as root, you should get the message "Press any button to start hashing.. ".
- **at this point create all the other brokers you wish to connect to root and return here to continue**
8. Press any key to start hashing (through a hash function it is determined which broker will be responsilble for each
   room/ topic).
9. You should get a list of the broker-IPs responsible for each topic/ room and the message "Broker is online and awaiting
   connections.." (*Return to the other brokers to finish the setup*).

#### Other Brokers

1. Open the `Broker` folder in your code editor.
2. If not automatically added, add the `LTest.jar` library from the folder `libs` as a dependency to 
   your code.
3. In the `Broker.java` file change the root ip on line 12 to the ip of of the system that the root broker
   will run on.
4. Run `Broker.java` in your terminal.
5. The `topics.txt` file contains all the chat rooms or conversation topics available for the users of the
   client app to subscribe to. Change its path on line 40 of `Broker.java` to that of your local folder.
6. Enter `4321` as the broker's Port Number when prompted.
7. Enter `n` when asked if you are connecting as root, you should get the message "Press any key when root is ready to start hashing..".
 - **at this point return to root broker terminal to continue**
8. Press any key to finish the setup, you should get the message "Broker is online and awaiting
   connections.."

You are finished with setting up the system brokers. Proceed to setting up the clients.

### Client

1. Open the DSApp folder in your IDE.
2. If you choose to run it via Android Studio, the dependencies will be loaded automatically. In any other case make sure
   to add the `LTest.jar` library from the folder `app/libs` as a dependency to your code.
3. Either run the app on a virtual emulator or run it on a connected Android phone in debugging mode.
4. The database used by the app is empty on installation and starts saving user data, messages and subscriptions through
   the UI.

## User Instructions

1. Make sure you didn't skip any of the steps above and the broker(s) is (are) up and running.
2. Open the app on your android device of choice.
3. If you are logging in for the first time you will be prompted to enter a username. Tap on `START`.
4. On the next page you can view all the conversations you are subscribed to. 
   a. Tap on a conversation to enter and view the content.
   b. To subscribe to new ones tap on `+`. On the upcoming menu all the topics hosted by the broker will be shown.
      Pick the ones you prefer and subscribe to them.
5. Enter the message you wish to send to this topic and tap on `->`. Else tap on &#x2191; to choose a file from your
   local storage and send it.
6. On the section above the "Enter Message" prompt you can view the conversation history since the first login.
   Everytime someone sends a message all the subscribers of a topic who are connected at that time receive it instantly.
   Else it is saved in the broker history and the user receives it upon entering the room..

