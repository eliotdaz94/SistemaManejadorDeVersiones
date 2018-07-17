# SistemaManejadorDeVersiones
Repositorio para el desarrollo de un Sistema Manejador de Versiones Distribuido (proyecto de la asignatura Sistemas de Operación II).

Para compilar:
	make

Para ejecutar:
	Ejecute primero el proceso MasterServer, luego ejecute el proceso StorageServer (tantas veces
	como se desee) y finalmente ejecute el proceso Client (en nodos distintos).

Para ejecutar el MasterServer:

	make runMaster masterPort masterAddress tolerance
		masterPort: Puerto por el que escuchará el MasterServer.
		masterAddress: Dirección IP de la máquina donde se ejecuta el MasterServer.
		tolerance: Enterog que indica la k-tolerancia.

Para ejecutar el StorageServer:

	make runStorage storagePort managmentPort storageAddress
		storagePort: Puerto por el que escuchará el StorageServer.
		managmentPort: Puerto cualquiera (parámetro que no se usa). 
		storageAddress: Dirección IP de la máquina donde se ejecuta el StorageServer.

Para ejecutar el Client:

	make runClient masterAddress masterPort storagePort clientAddress
		masterAddress: Dirección IP de la máquina donde se ejecuta el MasterServer.
		masterPort: Puerto por el que escuchará el MasterServer.
		storagePort: Puerto por el que escuchará el StorageServer.
		clientAddress: Dirección IP de la máquina donde se ejecuta el Client.