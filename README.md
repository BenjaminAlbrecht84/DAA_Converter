# DAA_Converter

This piece of code converts a MAF-File produced by LAST into a DIAMOND specific file format called DAA-File, which can then be loaded into MEGAN for example.

Given a query file together with a LAST database you basically have to conduct the following steps:
i. run LAST mapping the query on a database by reporting a MAF file
ii. convert the MAF file into a DAA file by using this tool
iii. run MEGAN for first meganizing the DAA file and then visualizing the assigned taxa  

## Downloading the program

Get the current version from [here](https://github.com/BenjaminAlbrecht84/DAA_Converter/releases/download/v0.8.3/DAA_Converter.jar).

## Running the program

### Mandatory Options:
 
Parameter | Description
--------- | -----------
-m  | path to MAF-File (can be also be piped-in, no gzip allowed)
-q  | path to query-file in FASTA or FASTQ format (can also be gzipped)
-d  | path of the reported DAA-file 

### Optional: 

Parameter | Description
--------- | -----------
-p  | number of available processors (default: maximal number)
-ps | number of available processors while input is piped-in (default: 1)
-t  | folder for temporary files (default: system's tmp folder)
-v  | enables verbose mode for reporting numbers of reads/references/alignments being analyzed

### Example:

Either the MAF-file is directly specfied, like

``java -jar DAA_Converter.jar -m <maf-file> -q <read-file>``

or the MAF-file is piped in from LAST, like

``lastal -P8 -F15 <last-db> <read-file> | java -jar DAA_Converter.jar  -q <read-file>``
