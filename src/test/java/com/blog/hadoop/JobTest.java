package com.blog.hadoop;

import com.blog.hadoop.mapreduce.CensusDataMapper;
import com.blog.hadoop.mapreduce.CensusDataReducer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class JobTest {
    public static final String INPUT_PATH = "input/input.txt";
    public static final String OUTPUT_PATH = "input/output.txt";
    static List<String> inputs;
    static List<String> outputs;
    MapReduceDriver mapReduceDriver;

    @BeforeClass
    public static void before() throws IOException {
        //read input and output
        inputs = Files.readAllLines(Paths.get(INPUT_PATH));
        outputs = Files.readAllLines(Paths.get(OUTPUT_PATH));
        outputs.remove(0);
    }

    @Before
    public void setUp() {
        Mapper mapper = new CensusDataMapper();
        Reducer reducer = new CensusDataReducer();
        mapReduceDriver = MapReduceDriver.newMapReduceDriver(mapper, reducer);
    }

    @Test
    public void testMapReduceIgnoreHeader() throws IOException {
        //input
        mapReduceDriver.withInput(new LongWritable(), new Text("@ssn,family_size,occupation,income"));
        mapReduceDriver.withInput(new LongWritable(), new Text("321 786 980,6,engineer,9000000"));

        //expected output
        mapReduceDriver.withOutput(new Text("engineer"), new Text("9000000,9000000,6,6"));

        //run job
        mapReduceDriver.runTest(false);
    }

    @Test
    public void testMapReduceWithNoHeader() throws IOException {
        //input
        mapReduceDriver.withInput(new LongWritable(), new Text("321 786 980,6,engineer,9000000"));

        //expected output
        mapReduceDriver.withOutput(new Text("engineer"), new Text("9000000,9000000,6,6"));

        //run job
        mapReduceDriver.runTest(false);
    }

    @Test
    public void testMapReduceStaticInput() throws IOException {
        //input
        for (String input : inputs) {
            mapReduceDriver.withInput(new LongWritable(), new Text(input));
        }

        //expected output
        for (String output : outputs) {
            String[] _output = output.split(",");
            mapReduceDriver.withOutput(new Text(_output[0]), new Text(output.substring(_output[0].length() + 1)));
        }

        //run job
        mapReduceDriver.runTest(false);
    }

    @Test
    public void testMapReduceDynamicInput() throws IOException {
        Random random = new Random();

        Map<String, List<Integer>> familySizes = new HashMap<>();
        Map<String, List<Integer>> incomes = new HashMap<>();

        String _input = "321 786 980,$FS,$OP,$IC";

        //input
        for (int i = 0; i < random.nextInt(50) + 10; i++) {
            String randOP = String.valueOf(random.nextInt(10) + 1);
            Integer randFM = random.nextInt(10) + 1;
            Integer randIC = random.nextInt(250000) + 1;

            Text in = new Text(_input.replace("$FS", String.valueOf(randFM)).replace("$IC", String.valueOf(randIC)).replace("$OP", String.valueOf(randOP)));

            mapReduceDriver.withInput(new LongWritable(), in);

            if (!familySizes.containsKey(randOP)) {
                familySizes.put(randOP, new ArrayList<>());
            }

            if (!incomes.containsKey(randOP)) {
                incomes.put(randOP, new ArrayList<>());
            }

            familySizes.get(randOP).add(randFM);
            incomes.get(randOP).add(randIC);
        }

        //expected output
        for (String op : familySizes.keySet()) {
            mapReduceDriver.withOutput(new Text(op), new Text(Collections.min(incomes.get(op)) + "," + Collections.max(incomes.get(op)) + "," + Collections.min(familySizes.get(op)) + "," + Collections.max(familySizes.get(op))));
        }

        //run job
        mapReduceDriver.runTest(false);
    }
}