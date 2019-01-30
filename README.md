# WoltAssignment
My solution to the wolt 2019 summer intern coding task. Calculates the median pickup times from the CSV file given in a very safe way.

## Usage
You need [sbt](https://www.scala-sbt.org/) to run this project.
This project is executed from the command line.

Formatting:

```sbt run [city] [date] [hours] [output file]```

For example if you wanted to get Helsinki's median pickup times from the 8th of january between the hours of 9 and 15, you'd write

```sbt run Helsinki 08-01-2019 9-15 mediantimes.csv``` 

City is always the folder from where the program will look for the csv files. Only "Helsinki" is supplied.

Date can be given in the following formats:

```DD-MM-YYYY``` or ```DD-MM-YY``` or ```YYYY-MM-DD```
