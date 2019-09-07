Prova rule language plugin for TIBCO StreamBase (Streaming) 10 
==============================================================

This is a closely integrated Prova Rule Language (https://github.com/prova/prova) plugin for TIBCO StreamBase 10.4.4 (currently, rebranded as TIBCO Streaming https://www.tibco.com/products/tibco-streaming).

## Features

- New operator for StreamBase called Prova; 
- The operator accepts rule base files written in Prova as source for StreamBase operators;
- The files can declaratively specify the input and output ports;
- The Prova operator can take parameters specified in StreamBase event flows ('fragments');
- All types are interactively type-checked on StreamBase Studio canvas for inbound and outbound flows;
- Sophisticated reactive event-processing constructs can be used in StreamBase (given the suitable Prova encoding, this includes formalisms like pi-calculus and Petri nets).

## Usage

- drop the available Prova operator on the canvas;
- wire up the inbound and outbound streams;
- declare the types of these streams;
- use reactive messaging and event processing constructs in Prova for processing the data, detecting event patterns, call Java or Scala functions and more.

## Documentation

Coming up.

## License

This code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0).

See the `NOTICE.txt` file for required notices and attributions.

