<div align="center">
  <a href="https://central.sonatype.com/artifact/io.github.spannm/jackcess"><img src="https://img.shields.io/maven-central/v/io.github.spannm/jackcess?label=Maven%20Central&style=flat-square" alt="Maven Central Version"></a>
  <img src="https://img.shields.io/maven-central/last-update/io.github.spannm/jackcess?label=Updated&style=flat-square&color=blue" alt="Maven Central Last Update">
  <a href="https://github.com/spannm/jackcess/stargazers"><img src="https://img.shields.io/github/stars/spannm/jackcess?logo=github&label=&logoColor=white&labelColor=555555&color=007ec6&style=flat-square" alt="GitHub Stars"></a>
  <br>
  <a href="https://github.com/spannm/jackcess/actions/workflows/ci_jdk11_ubuntu.yml"><img src="https://img.shields.io/github/actions/workflow/status/spannm/jackcess/ci_jdk11_ubuntu.yml?label=Build%20(JDK%2011%20Linux)&style=flat-square" alt="GitHub Actions Workflow Status"></a>
  <a href="https://github.com/spannm/jackcess/actions/workflows/ci_jdk11_win.yml"><img src="https://img.shields.io/github/actions/workflow/status/spannm/jackcess/ci_jdk11_win.yml?label=Build%20(JDK%2011%20Win)&style=flat-square" alt="GitHub Actions Workflow Status"></a>
  <a href="https://javadoc.io/doc/io.github.spannm/jackcess"><img src="https://javadoc.io/badge2/io.github.spannm/jackcess/javadoc.svg?style=flat-square" alt="Javadoc"></a>
  <a href="https://apidia.net/mvn/io.github.spannm/jackcess"><img src="https://apidia.net/mvn/de.speedbanking/iban-commons/badge_flat_square.svg" alt="APIdia"></a>
</div>

<h1 align="center">Welcome to Jackcess</h1>
<h3 align="center">The Pure Java API for Microsoft Access Databases</h3>

**Jackcess** is an open-source Java library for reading from and writing to Microsoft Access databases (`.mdb` and `.accdb`).

Unlike JDBC-based solutions, Jackcess provides a direct, low-level API to manipulate Access files without the overhead of a database engine. It is the library powering [UCanAccess](https://github.com/spannm/ucanaccess).

Jackcess is not an application. There is no GUI. It's a library, intended for other developers to build Java applications.

<div align="center"> ──────────────────── </div>

## ✨ Key Features

* **Pure Java**: 100% Java implementation. No native dependencies, no DLLs, no MS Access installation required.

* **Wide Version Support**: Supports Access versions 2000, 2003, 2007, 2010, 2013, 2016, and 2019.

* **Read & Write**: Create tables, insert rows, update data, and read complex schemas directly from the file.

* **Schema Manipulation**: Programmatically create or modify table structures, indexes, and relationships.

<p style="height: 20px;">&nbsp;</p>

## 🛠 Tech Stack & Dependencies

* **Java Version**: 11 or higher (LTS versions like Java 17 and 21 are fully supported and tested).

* **Main Dependency**:
  * [Apache POI](https://poi.apache.org/) (for encryption support and internal file handling)

* **Build Tool**: [Maven](https://maven.apache.org/)

* **Code Quality**: Enforced via Checkstyle, PMD, and SpotBugs.

<p style="height: 20px;">&nbsp;</p>

## 📦 Installation

To use Jackcess in your project, add the following dependency.

### Maven (`pom.xml`)

```xml
<dependency>
    <groupId>io.github.spannm</groupId>
    <artifactId>jackcess</artifactId>
    <version>5.1.4</version>
</dependency>
```

### Gradle (Groovy / `build.gradle`)

```groovy
implementation 'io.github.spannm:jackcess:5.1.4'
```

## 🚦 Usage Example

Jackcess is a library for developers. Here is a quick look at the API for reading rows:

```java
import io.github.spannm.jackcess.*;

import java.io.File;

try (Database db = DatabaseBuilder.open(new File("database.accdb"))) {
    Table table = db.getTable("friends");
    for (Row row : table) {
        System.out.println("Friend Name: " + row.get("name"));
    }
}

```

## ❤️ Origin & Maintenance

This project is a modern fork of the original [Jackcess project on SourceForge](https://sourceforge.net/projects/jackcess/), originally created and maintained by [James Ahlborn](https://sourceforge.net/u/jahlborn/profile/),
specifically created to ensure compatibility with modern Java versions, minimize dependencies and to serve as the foundation for [UCanAccess](https://github.com/spannm/ucanaccess).

### ⚖️ License

Jackcess is licensed under the **Apache License, Version 2.0**.

<p style="height: 40px;">&nbsp;</p>

<div align="center">
<table style="border-collapse: collapse;">
  <tr>
    <td style="padding: 40px; border: 2px solid #3a82c2;">
      <strong>Enjoying Jackcess? Please leave a 🌟 to support the project!</strong>
    </td>
  </tr>
</table>
</div>
