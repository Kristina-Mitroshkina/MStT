package jade.agents.ordering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class ItemList {
    private List<PartItemList> _partLists;

    public ItemList(final List<PartItemList> partLists) {
        _partLists = partLists;
    }

    public ItemList(String fileName) {
        List<List<String>> grid = readFromCsvFile(fileName);
        _partLists = stringGridToItemlist(grid);
    }

    public ItemList(String input, boolean distinguishFromOtherConstructor) {
        List<List<String>> grid = readFromString(input);
        _partLists = stringGridToItemlist(grid);
    }

    public List<PartItemList> getPartLists() {
        return _partLists;
    }

    public double getPriceInDollars() {
        double priceInDollars = 0.0;
        for (PartItemList partOrder : _partLists) {
            priceInDollars += partOrder.getPriceInDollars();
        }
        return priceInDollars;
    }

    private static final int ITEMS_PER_ROW = 2;

    private static List<List<String>> readFromCsvFile(String fileName) {
        List<List<String>> grid = new ArrayList<List<String>>();
        int lineNumber = 0;
        try {
            BufferedReader input = new BufferedReader(new FileReader(new File(fileName)));
            try {
                String line;
                while ((line = input.readLine()) != null) {
                    //System.out.println(fileName + " " + lineNumber + ": '" + line + "'");
                    List<String> row = new ArrayList<String>();
                    StringTokenizer tokenizer = new StringTokenizer(line, ",");
                    while (tokenizer.hasMoreTokens()) {
                        row.add(tokenizer.nextToken().trim());
                    }
                    if (row.size() < ITEMS_PER_ROW) {
                        System.err.println(fileName + ":" + (lineNumber + 1) + " has less than " + ITEMS_PER_ROW + " items: " + line);
                    }
                    //System.out.println("** " + row.get(0) + "," + row.get(1) + "," + row.get(2) + ",");
                    grid.add(row);
                    ++lineNumber;
                }
            } finally {
                input.close();
                System.out.println("Read " + fileName + " " + lineNumber + " lines ------------------------");
            }
        } catch (IOException e) {
            System.err.println("Error opening " + fileName + ": " + e);
        }
        return grid;
    }

    private static List<List<String>> readFromString(String input) {
        List<List<String>> grid = new ArrayList<List<String>>();
        int lineNumber = 0;

        StringTokenizer tokenizerOuter = new StringTokenizer(input, ";");
        while (tokenizerOuter.hasMoreTokens()) {
            String line = tokenizerOuter.nextToken().trim();
            List<String> row = new ArrayList<String>();
            StringTokenizer tokenizer = new StringTokenizer(line, ",");
            while (tokenizer.hasMoreTokens()) {
                row.add(tokenizer.nextToken().trim());
            }
            if (row.size() < ITEMS_PER_ROW) {
                System.err.println(" " + (lineNumber + 1) + " has less than " + ITEMS_PER_ROW + " items: " + line);
            }
            grid.add(row);
            ++lineNumber;
        }
        return grid;
    }


    private static List<PartItemList> stringGridToItemlist(List<List<String>> grid) {
        List<PartItemList> partLists = new ArrayList<PartItemList>();
        for (List<String> row : grid) {
            assert (row.size() >= ITEMS_PER_ROW);
            try {
                String name = row.get(0);
                int number = Integer.parseInt(row.get(1));
                double priceInDollars = 0.0;
                if (row.size() > 2) {
                    priceInDollars = Double.parseDouble(row.get(2));
                }
                PartItemList part = new PartItemList(name, priceInDollars, number);
                partLists.add(part);
            } catch (Exception e) {
                System.err.println("Error parsing string:" + e);
            }
        }
        return partLists;
    }

    public PartItemList getItemByName(String name) {
        for (PartItemList p : _partLists) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public boolean contains(final ItemList otherList) {
        for (PartItemList otherPart : otherList.getPartLists()) {
            PartItemList thisPart = getItemByName(otherPart.getName());
            if (thisPart == null) {
                return false;
            } else if (thisPart.getNumber() < otherPart.getNumber()) {
                return false;
            }
        }
        return true;
    }

    public void subtract(final ItemList otherList) {
        for (PartItemList otherPart : otherList.getPartLists()) {
            PartItemList thisPart = getItemByName(otherPart.getName());
            // Must be guaranteed by a check against contains before calling this function
            assert (thisPart != null && thisPart.getNumber() >= otherPart.getNumber());
            thisPart.setNumber(thisPart.getNumber() - otherPart.getNumber());
        }
    }

    public ItemList getCostedOrder(final ItemList order) {
        List<PartItemList> costedPartLists = new ArrayList<PartItemList>();
        for (PartItemList orderPart : order.getPartLists()) {
            PartItemList thisPart = getItemByName(orderPart.getName());
            // Must be guaranteed by a check against contains before calling this function
            assert (thisPart != null && thisPart.getNumber() >= orderPart.getNumber());
            PartItemList costedPart = new PartItemList(orderPart.getName(), thisPart.getUnitPriceInDollars(), orderPart.getNumber());
            costedPartLists.add(costedPart);
        }
        return new ItemList(costedPartLists);
    }

    public String getAsString() {
        String itemListString = "";
        for (PartItemList part : getPartLists()) {
            String partString = part.getName() + "," + part.getNumber() + "," + part.getUnitPriceInDollars();
            itemListString += partString + ";";
        }
        return itemListString;
    }
}
