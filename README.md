A Java client used to query the GeoNet Continuous Waveform Buffer.

See: http://info.geonet.org.nz/display/appdata/Waveform+Data

### Build with Gradle

`./gradlew jar` - outputs a jar to `build/libs/GeoNetCWBQuery-4.2.0-GRADLE.jar`
`./gradlew repack` - outputs a proguard compressed jar to `build/libs/repacked-4.2.0-GRADLE.jar`

run the jar (outputs SAC format data)

`java -jar repacked-4.2.0-GRADLE.jar -event geonet:3272673`

