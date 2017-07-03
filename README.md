# DAA_Converter

This piece of code converts MAF-Files produced by LAST to DAA-Files, which can then be loaded into MEGAN for example.

## Downloading the program

Get the current version frome [here](https://github.com/BenjaminAlbrecht84/DAA_Converter/releases/download/v0.8.2/DAA_Converter.jar).

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
