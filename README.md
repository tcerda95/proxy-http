# proxy-http

## Ubicación de Archivos

En el directorio raíz se encuentra el directorio **proxy-http** que contiene el código fuente como también el archivo de construcción pom.xml.
En el directorio raíz se encuentra también el informe con el nombre informe.pdf y el archivo de presentación presentación.pdf.

## Construcción y ejecución

Ejectuar el comando `mvn package` dentro del directorio **proxy-http**.
Se generará el directorio **proxy-http/target** donde en el directorio **proxy-http/target/lib** se encuentran las dependencias.
Para correr la aplicación, ejecutar el comando `java -jar proxy-http-1.0.java` dentro del directorio **proxy-http/target**.
En el directorio **proxy-http/target/logs** se generarán los archivos de logs.

## Configuración

Para configurar puertos y tamaño de buffers, se puede abrir el archivo **proxy-http/target/proxy-http-1.0.java** con un editor de textos como vim y modificar el archivo de configuración **proxy.properties**. Se describen a continuación las diferentes configuraciones:

* **proxy.port:** puerto de la aplicación proxy
* **proxy.bufferSize:** tamaño default de buffers de transferencia del proxy
 
* **protocol.port:** puerto de la aplicación del protocolo
* **protocol.bufferSize:** tamaño de buffers de transferencia del protocolo
* **protocol.parser.bufferSize:** tamaño de buffer del parser del protocolo
* **protocol.parser.headerNameBufferSize:** tamaño de buffer para nombre del header del protocolo
* **protocol.parser.headerContentBufferSize:** tamaño de buffer para el contenido de un header
 
* **parser.methodBufferSize:** tamaño del buffer para el método HTTP
* **parser.requestLineBufferSize:** tamaño del buffer de primer línea de la request HTTP
* **parser.URIHostBufferSize:** tamaño del buffer que guarda host si se encuentra en la primer línea
* **parser.headerNameBufferSize:** tamaño del buffer del nombre de headers HTTP
* **parser.headerContentBufferSize:** tamaño del buffer del contenido de headers HTTP
 
* **connection.queue.length:** cantidad de conexiones que se persisten de un mismo par host:puerto
* **connection.ttl:** time to live en segundos de una conexión persistida
* **connection.clean.rate:** cada cuantos segundos se realiza una limpieza de conexiones cuyo ttl expiró