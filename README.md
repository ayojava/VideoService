# VideoService
Simple VideoService Implementation using the Play Framework.

The VideoService provides a JSON REST API to access videos.
Via this API the client can
- upload videos
- download videos
- rate videos
- obtain a list of available videos
- delete videos

There are currently two implementations of the service,
one using the Play-Java API with Ebean persistence and
the other one using the Play-Scala-API with Slick persistence.

The third project is an external test client which requires that
you first start a VideoServer and run the tests afterwards.
The Tests are written in Java using the Retrofit library.

Each project directory contains a README-HowTo.txt which gives
a very short intro into the respective project and explains
how to run it, how to test it and how to import it into your IDE.

Have fun.

Hermann
