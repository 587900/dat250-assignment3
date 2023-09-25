# DAT250: Assignment 3
By 587900 (Kjetil Berg)

### Description
The instructions of https://github.com/selabhvl/dat250public/blob/master/expassignments/expass3.md was followed to create a mongodb interaction experiment.
We were free to use the tools we desired (for example Mongodb Compass), however I opted to use Java.
Everything went according to the instructions besides what is mention under the 'Trials and tribulations' section (problems and noteworthy moments).

### Tools
I downloaded mongodb from https://www.mongodb.com/try/download/community (v4.4.24, windows x64, msi)
and the file checksum from https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-4.4.24-signed.msi.sha256.
I tested the checksum using the commands shown in the image below (powershell):\
![mongodb checksum verification](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/checksum.png)

### Results
During experiment 1 and 2, I took some screenshots of my results. They might seem a bit lack luster in experiment 1.
For a more complete picture, see the java file in this repository. Experiment 1 (in order: insert, query, update, delete, bulk-write):\
![insert](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/insert.png)
![query](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/query.png)
![update](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/update.png)
![delete](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/delete.png)
![bulk write](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/bulk-write.png)\
Experiment 2 (split in two images):\
![aggregate part 1](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/aggregate-part1.png)
![aggregate part 2](https://raw.githubusercontent.com/587900/dat250-assignment3/master/img/aggregate-part2.png)\
Again, see the java file in this repository. You could also run it yourself. By default it will connect to localhost:27017, db: 'dat250-assignment3' and use collection 'collection'.
Note: The program will clear the contents of the configured collection when it is run.

### Trials and tribulations
1. I tried installing mongodb. Apparently it is not compatible with Windows 8.1 (which I was using on my laptop).
Why mongodb requires Windows 10 is beyond me. This was quite troublesome and caused a long delay in me finishing this exercise.
2. I needed to poke around mongodb's website to find their file checksum for verification purposes (which we must do according to the instructions).
It wasn't too bad, but I feel it should be right next to the download link.
3. The mongodb driver for java wanted to use slf4j. I poked around and found some maven dependencies. I also discovered that slf4j requires two dependencies: an api and a provider.
Finding a proper "provider" took a bit of time. I went through 4 different providers before I found one that wouldn't just shit in the console.
4. Experiment 1 went without issue.
5. Experiment 2 went mostly fine (both mapReduce and aggregate), however two notable moments came up:\
5.1: It was a hassle to manually enter the data in 'java code'. Many of mongodb's sites let you pick a language and get relevant snippets,
however not the aggregation site. I eventually came across a `Document.parse` method that was useful, since I could then just copy the json from mongodb's site.
I had to manually parse the dates, though, because the java driver for mongodb expected a different date format. I wrote a helper function for that.\
5.2: The 2nd aggregate problem was a bit larger and for some reason I did not get any output. I looked around and found out that Mongodb Compass has a feature cleverly named
"export an aggregation pipeline to another language". I used this to make some java code that I could test. This helped me discover my bug - The "greater than date" took a Java date.
I had used my old helper function. Unfortunately, I had forgotten that it was doing String -> String instead of String -> Date. I wrote a new helper function to solve that.
6. I noticed that the mapReduce method uses String literals for passing in functions (define a String with code that is a function). The method was also deprecated, probably for the same reason.

### Why Aggregation / MapReduce is useful
Sometimes data is split across entries and can be more easily handled in an application when it is 'aggregated'.
Note that MapReduce is just a method of aggregating data. The aggregation step could always be done by the server or application that
requires the aggregated data, however there are some benefits to having this be done on this way:
1. Less data may need to be transmitted if the individual entries that make up the aggregated data is large.
2. Implementing a custom method for aggregating individual entries may be costly and time consuming. Using mongodb's built-in aggregation engine(s) makes this easier for development.

### Analyzing the Aggregated data:
Note: See the pictures in the 'Results' section.
The output is the same as shown in the aggregation examples (linked in the instructions).
For the "Total price per customer" aggregation example, we see that the result very simply defines a collection that maps customers (_id) to a value.
The "Order and total quantity with average quantity per item" is a bit more complicated, but the result is all the same: We see a collection that maps items (_id) to a document containing a count, quantity and average (count = number of orders, qty = total quantity, avg = average quantity per order).
Note that in this repository's java code, you can also see what orders they map to in the aggregation example by uncommenting a line of code.
