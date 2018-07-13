make:
	javac Client.java MasterServer.java StorageServer.java Message.java \
		  MulticastServer.java FileVersion.java

rm:
	rm *.class