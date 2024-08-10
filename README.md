With the release of GridDB v5.6, we are taking a look at the new features that come bundled with this new update. To read the entirety of the notes, you can read them directly from GitHub: GridDB CE v5.6 Release Notes.

Of the new features, today we are focusing on the new data compression algorithm that is now selectable in the gs_node.json config file. Prior to v5.6, there were only two methods of compression that were selectable: NO_COMPRESSION and COMPRESSION_ZLIB. Though the default setting is still no compression for all versions, version 5.6 offers a new compression method called COMPRESSION_ZSTD.

This compression method promises to be more efficient at compressing your data regularly, and also at compressing the data itself, meaning we can expect a smaller footprint. So, in this article, we will inserting a consistent amount of data into GridDB, comparing the resulting storage space taken up, and then finally comparing between all three compression methods.

Another feature we would like to go over quickly is regarding the CLI. With v5.6, the GridDB team released the ability to save variables within the CLI. And though this may seem minor, we will do a quick look at an unexpected benefit of this feature.

Methodology
As explained above, we will need to easily compare between three instances of GridDB with the same dataset. To accomplish this, it seems docker would be the easiest method because we can easily spin up or down new instances and change the compression method for each instance. If we do this, then we simply use the same dataset or the same data generation script for each of the instances.

To get a robust enough dataset to really test the compression alogrithm differences, we decided on 100 million rows of data. Specifically, we wanted the dataset to be similar enough in some respects that the compression can do its job so that we in turn can effectively measure its effectiveness.

The three docker containers will be griddb-server1, griddb-server2, and griddb-server3. The compression levels are set in the docker-compose file, but we will do it the way that makes the most sense to me: server1 is NO_COMPRESSION, server2 is the old compression system (COMPRESSION_ZLIB), and server3 is the new compression system (COMPRESSION_ZSTD).

So when we run our gen-script, we can use command line arguments to specify which container we want to target. More on that in the next section.

How to Follow Along
If you plan to build and test out these methods yourself while you read along, you can grab the source code from our GitHub page: .

Once you have the repo, you can start with spinng up your GridDB servers. We will get into how to run the generation data script to push 100m rows of data into your servers in the next section.

To get the three servers running, the instructions are laid out in the docker compose file in the root of the projectory repoistory, so simply run:

$ docker compose build
$ docker compose up -d
If all goes well, you should have three GridDB containers running: griddb-server1, griddb-server2, griddb-server3

Implementation
To implement, we used a node.js script which generated 100m rows of random data. Because our GridDB containers are spun up using Docker, we made all three docker containers for GridDB separate services inside of a docker compose file. We then grabbed that docker network name and used it when running our nodejs script.

This means that our nodejs script was also built into a docker container and then we used that to push data into the GridDB containers with the following commands: