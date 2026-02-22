package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class App {
    static Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8.name());

    public static Map<String, Set<Integer>> buildIndex(String[][] inp, int N) {
        Map<String, Set<Integer>> index = new HashMap<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                String field = inp[i][j];
                if (field == null || field.isEmpty()) continue;

                String[] words = field.toLowerCase(Locale.ROOT).split(" ");
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    index.putIfAbsent(word, new HashSet<>());
                    index.get(word).add(i);
                }
            }
        }
        return index;
    }

    public static void printRow(int index, String[][] arr) {
        String first = arr[index][0];
        String last = arr[index][1];
        String email = arr[index][2];

        if (email == null || email.isEmpty()) {
            System.out.println(first + " " + last);
        } else {
            System.out.println(first + " " + last + " " + email);
        }
    }

    public static void printPpl(String[][] arr, int N) {
        System.out.println("=== List of people ===");
        for (int i = 0; i < N; i++) {
            printRow(i, arr);
        }
    }

    public static String getDataFileName(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--data".equals(args[i])) return args[i + 1];
        }
        return null;
    }

    //Strategy Search Helpers

    public static Set<Integer> getAnyMatches(String queryLine, Map<String, Set<Integer>> index) {
        Set<Integer> result = new HashSet<>();
        String[] words = queryLine.toLowerCase(Locale.ROOT).trim().split(" ");

        for (String w : words) {
            if (w.isEmpty()) continue;
            Set<Integer> rows = index.get(w);
            if (rows != null) result.addAll(rows);
        }
        return result;
    }

    public static Set<Integer> getAllMatches(String queryLine, Map<String, Set<Integer>> index) {
        String[] words = queryLine.toLowerCase(Locale.ROOT).trim().split(" ");

        Set<Integer> result = null; // start null, then intersect
        for (String w : words) {
            if (w.isEmpty()) continue;

            Set<Integer> rows = index.get(w);
            if (rows == null) {
                return new HashSet<>(); // one word missing => no ALL match
            }

            if (result == null) { //IF FIRST WORD YOU'RE SEARCHING, CREATE ANEW MAP
                result = new HashSet<>(rows); 
            } else {
                result.retainAll(rows); // IF NOT PERFORM AND OPERATION
            }
        }

        return result == null ? new HashSet<>() : result;
    }

    public static Set<Integer> getNoneMatches(String queryLine, Map<String, Set<Integer>> index, int N) {
        Set<Integer> any = getAnyMatches(queryLine, index);

        Set<Integer> result = new HashSet<>();
        for (int i = 0; i < N; i++) result.add(i);

        result.removeAll(any);
        return result;
    }

    public static void searchPpl(String[][] inp, int N, Map<String, Set<Integer>> index) {
        System.out.println("Select a matching strategy: ALL, ANY, NONE");
        String strategy = sc.nextLine().trim().toUpperCase(Locale.ROOT);

        System.out.println("Enter a name or email to search all suitable people.");
        String queryLine = sc.nextLine();

        Set<Integer> results;
        switch (strategy) {
            case "ALL":
                results = getAllMatches(queryLine, index);
                break;
            case "ANY":
                results = getAnyMatches(queryLine, index);
                break;
            case "NONE":
                results = getNoneMatches(queryLine, index, N);
                break;
            default:
                results = new HashSet<>();
                break;
        }

        System.out.println(results.size() + " persons found:");
        for (int row : results) {
            printRow(row, inp);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        String fileName = getDataFileName(args);
        if (fileName == null) return;

        File fl = new File(fileName);

        // Count lines
        int N = 0;
        Scanner counter = new Scanner(fl, StandardCharsets.UTF_8.name());
        while (counter.hasNextLine()) {
            String line = counter.nextLine().trim();
            if (!line.isEmpty()) N++;
        }
        counter.close();

        // Read into array
        String[][] inp = new String[N][3];
        Scanner scanner = new Scanner(fl, StandardCharsets.UTF_8.name());

        int i = 0;
        while (scanner.hasNextLine() && i < N) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(" ");
            inp[i][0] = parts.length > 0 ? parts[0] : "";
            inp[i][1] = parts.length > 1 ? parts[1] : "";
            inp[i][2] = parts.length > 2 ? parts[2] : "";
            i++;
        }
        scanner.close();

        Map<String, Set<Integer>> index = buildIndex(inp, N);

        while (true) {
            System.out.println("=== Menu ===");
            System.out.println("1. Find a person");
            System.out.println("2. Print all persons");
            System.out.println("0. Exit");

            int option = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (option) {
                case 0:
                    System.out.println("Bye!");
                    return;
                case 1:
                    searchPpl(inp, N, index);
                    break;
                case 2:
                    printPpl(inp, N);
                    break;
                default:
                    System.out.println("Incorrect option! Try again.");
            }
        }
    }
}
