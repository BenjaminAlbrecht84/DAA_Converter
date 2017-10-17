# DAA_Converter

This piece of code converts a MAF-File produced by LAST into a DIAMOND specific file format called DAA-File, which can then be loaded into MEGAN for example.

Given a query file together with a LAST database you basically have to conduct the following steps:

1. Run LAST mapping the query on a database by reporting a MAF file.
2. Convert the MAF file into a DAA file by using this tool.
3. Run MEGAN for first meganizing the DAA file and then visualizing the assigned taxa.

## Downloading the program

Get the current version from [here](https://github.com/BenjaminAlbrecht84/DAA_Converter/releases/download/v0.9.0/DAA_Converter_v0.9.0.jar).

## Running the program

### Mandatory Options:
 
Parameter | Description
--------- | -----------
-i, --in  | sets path to MAF-File (can also be piped in, no gzip allowed here)
-r, -- reads  | sets path to query-file in FASTA or FASTQ format (can also be gzipped)
-o, --out  | sets path of the reported DAA-File

### Optional: 

Parameter | Description
--------- | -----------
-top, --topPercent | sets top percent of reads kept during filtering (default: 10.0)
--p, --procs | sets number of used processors (default: maximal number)

Parameter | Description
--------- | ----------
-ps, --streamingProcs | sets number of used processors while input is piped-in (default: 1)
-cs, --chunkSize |sets chunk-size of temporary MAF files (default: 500mb)
--t, --tmp  | sets folder for temporary files (default: parent folder of the resulting DAA-File)

Parameter | Description
--------- | -----------
-v, --verbose | sets verbose mode reporting numbers of reads/references/alignments being analyzed
-h, --help |	shows program usage and quits

### Example:

Either the MAF-file is directly specfied, like

``java -jar DAA_Converter.jar -m <maf-file> -q <read-file>``

or the MAF-file is piped in from LAST, like

``lastal -P8 -F15 <last-db> <read-file> | java -jar DAA_Converter.jar  -q <read-file>``

Notice that the latter variant of calling the converter has the advantage that no big MAF-file has to be stored on disk. Here, each chunk of the MAF-file is directly filtered and then compressed into a DAA-file. In the end all the resulting DAA-files are merged into one single DAA-File.
