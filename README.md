Debloat is an Open-Source, extensible, Enterprise-ready data compression/decompression framework.

It supports several compression algorithms, and more can be easily added using properties files, XML documents or even programmatically with a simple declarative DSL.

The typical problem with existing data compression applications and libraries is that the output they produce often consists in opaque binary blobs, which nobody can inspect to see what's inside, and it's very hard to write tools that can operate on those blobs. Debloat solves that by using a new codec that produces easily parsed, human-readable XML files!

For example, assume you would like to compress the following string:

`Trololololo lololo lololo.`

A regular compression library would produce something that perhaps looks like this:

`▼ï◘┘üöO♥♂)╩╧üB♣¶JÅ♂ê╝P!←`

You can see that this is a complete opaque bag of random symbols corresponding to various binary values. However, Debloat produces well-formed output that looks like this:

```
<?xml version="1.0" encoding="UTF-8"?>
<compressedData algorithm="lz77">
  <byte value="84"/>
  <byte value="114"/>
  <byte value="111"/>
  <byte value="108"/>
  <reference distance="2" length="7"/>
  <byte value="32"/>
  <reference distance="7" length="13"/>
  <byte value="46"/>
  <byte value="10"/>
</compressedData>
```

Undoubtedly, this format has many, many advantages for Java Enterprise applications respect to the opaque binary blob.
