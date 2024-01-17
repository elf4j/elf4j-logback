# elf4j-logback

An adapter to use [LOGBACK](https://logback.qos.ch/) as service provider and runtime log engine
for the [ELF4J](https://github.com/elf4j/elf4j) (Easy Logging Facade for Java) API

## User Story

As an application developer using the [ELF4J](https://github.com/elf4j/elf4j) API, I want to have the option of
selecting [LOGBACK](https://logback.qos.ch/) as my log engine, at application deploy time without code change or
re-compile.

## Prerequisite

Java 8+

## Get It...

[![Maven Central](https://img.shields.io/maven-central/v/io.github.elf4j/elf4j-logback.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.elf4j%22%20AND%20a:%22elf4j-logback%22)

## Use It...

If you are using the [ELF4J API](https://github.com/elf4j/elf4j) for logging, and wish to select or change to use
LOGBACK as the run-time log engine, then simply pack this service provider in the classpath when the application
deploys. No code change needed. At compile time, the client code is unaware of this run-time logging service provider.
With the ELF4J facade, opting for LOGBACK as the logging implementation is a deployment-time decision.

The usual [LOGBACK configuration](https://logback.qos.ch/manual/configuration.html) applies.

With Maven, in addition to use compile-scope on the [ELF4J API](https://github.com/elf4j/elf4j) dependency, an end-user
application would use runtime-scope for this provider as a dependency:

```html

<dependency>
    <groupId>io.github.elf4j</groupId>
    <artifactId>elf4j</artifactId>
    <scope>compile</scope>
</dependency>

<dependency>
    <groupId>io.github.elf4j</groupId>
    <artifactId>elf4j-logback</artifactId>
    <scope>runtime</scope>
</dependency>
```
