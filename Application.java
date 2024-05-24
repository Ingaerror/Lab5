import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Scanner;

/**
 * Главный класс.
 */
public class Application {
    private final LinkedHashSet<LabWork> labWorks;
    private final String fileName;
    private final Deque<String> commandHistory;
    private final Date initDate;
    private boolean exit = false;

    public Application(String fileName) {
        this.labWorks = new LinkedHashSet<>();
        this.fileName = fileName;
        this.commandHistory = new LinkedList<>();
        this.initDate = new Date();
        loadFromFile();
    }

    private void loadFromFile() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final LabWork labWork = LabWork.fromCsv(line);
                labWorks.add(labWork);
            }
        } catch (IOException | ParseException e) {
            System.err.println("Ошибка при чтении CSV-файла: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (LabWork labWork : labWorks) {
                writer.println(labWork.toCsv());
            }
        } catch (IOException e) {
            System.err.println("Ошибка записи в файл: " + e.getMessage());
        }
    }

    private void addCommandToHistory(String command) {
        if (commandHistory.size() == 11) {
            commandHistory.removeFirst();
        }
        commandHistory.add(command);
    }

    public void help() {
        System.out.println("Доступные команды:");
        System.out.println("help : вывести справку по доступным командам");
        System.out.println("info : вывести в стандартный поток вывода информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)");
        System.out.println("show : вывести в стандартный поток вывода все элементы коллекции в строковом представлении");
        System.out.println("add {element} : добавить новый элемент в коллекцию");
        System.out.println("update id {element} : обновить значение элемента коллекции, id которого равен заданному");
        System.out.println("remove_by_id id : удалить элемент из коллекции по его id");
        System.out.println("clear : очистить коллекцию");
        System.out.println("save : сохранить коллекцию в файл");
        System.out.println("execute_script file_name : считать и исполнить скрипт из указанного файла");
        System.out.println("exit : завершить программу (без сохранения в файл)");
        System.out.println("add_if_max {element} : добавить новый элемент в коллекцию, если его значение превышает значение наибольшего элемента этой коллекции");
        System.out.println("remove_greater {element} : удалить из коллекции все элементы, превышающие заданный");
        System.out.println("history : вывести последние 11 команд (без их аргументов)");
        System.out.println("remove_any_by_difficulty difficulty : удалить из коллекции один элемент, значение поля difficulty которого эквивалентно заданному");
        System.out.println("print_unique_difficulty : вывести уникальные значения поля difficulty всех элементов в коллекции");
        System.out.println("print_field_ascending_discipline : вывести значения поля discipline всех элементов в порядке возрастания");
    }

    public void info() {
        System.out.println("Тип коллекции: " + labWorks.getClass().getName());
        System.out.println("Дата инициализации: " + initDate);
        System.out.println("Количество элементов: " + labWorks.size());
    }

    public void show() {
        for (LabWork labWork : labWorks) {
            System.out.println(labWork);
        }
    }

    public void add(LabWork labWork) {
        labWorks.add(labWork);
    }

    public void update(int id, final Scanner scanner) {
        Optional<LabWork> labWorkOptional = labWorks.stream()
                .filter(labWork -> labWork.getId() == id)
                .findFirst();

        if (labWorkOptional.isPresent()) {
            LabWork existingLabWork = labWorkOptional.get();
            System.out.println("Текущие значения полей элемента: ");
            System.out.println(existingLabWork);

            System.out.println("Введите новые значения полей. Поля с автоматической генерацией будут пропущены.");
            LabWork updatedLabWork = readLabWorkFromConsole(existingLabWork, scanner);

            // Обновляем элемент в коллекции
            labWorks.remove(existingLabWork);
            labWorks.add(updatedLabWork);

            System.out.println("Элемент успешно обновлен.");
        } else {
            System.out.println("Элемент с id " + id + " не найден.");
        }
    }

    public void removeById(int id) {
        labWorks.removeIf(labWork -> labWork.getId() == id);
    }

    public void clear() {
        labWorks.clear();
    }

    public void addIfMax(LabWork labWork) {
        Optional<LabWork> max = labWorks.stream().max(Comparator.naturalOrder());
        if (max.isPresent() && labWork.compareTo(max.get()) > 0) {
            labWorks.add(labWork);
        }
    }

    public void removeGreater(LabWork labWork) {
        labWorks.removeIf(existingLabWork -> existingLabWork.compareTo(labWork) > 0);
    }

    public void history() {
        for (String command : commandHistory) {
            System.out.println(command);
        }
    }

    public void removeAnyByDifficulty(Difficulty difficulty) {
        for (LabWork labWork : labWorks) {
            if (labWork.getDifficulty() == difficulty) {
                labWorks.remove(labWork);
                break;
            }
        }
    }

    public void printUniqueDifficulty() {
        System.out.println("Уникальные значения сложности:");
        labWorks.stream()
                .map(LabWork::getDifficulty)
                .distinct()
                .forEach(System.out::println);
    }

    public void printFieldAscendingDiscipline() {
        System.out.println("Дисциплины в порядке возрастания:");
        labWorks.stream()
                .map(LabWork::getDiscipline)
                .sorted(Comparator.comparing(Discipline::getName))
                .forEach(System.out::println);
    }

    public void executeScript(String scriptFileName) {
        try (FileInputStream inputStream = new FileInputStream(scriptFileName);
             Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine() && !exit) {
                String command = scanner.nextLine();
                executeCommand(command, scanner);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла скрипта: " + e.getMessage());
        }
    }

    public void executeCommand(String command, Scanner scanner) {
        addCommandToHistory(command);

        String[] commandParts = command.split(" ");
        String commandName = commandParts[0];
        switch (commandName) {
            case "help":
                help();
                break;
            case "info":
                info();
                break;
            case "show":
                show();
                break;
            case "add":
                LabWork newLabWork = readLabWorkFromConsole(null, scanner);
                add(newLabWork);
                System.out.println("Новый элемент успешно добавлен.");
                break;
            case "update":
                if (commandParts.length < 2) {
                    System.out.println("Не указан id элемента для обновления.");
                    break;
                }
                try {
                    int id = Integer.parseInt(commandParts[1]);
                    update(id, scanner);
                } catch (NumberFormatException e) {
                    System.out.println("Неверный формат id. Попробуйте еще раз.");
                }
                break;
            case "remove_by_id":
                if (commandParts.length < 2) {
                    System.out.println("Не указан id элемента для удаления.");
                    break;
                }
                try {
                    int id = Integer.parseInt(commandParts[1]);
                    removeById(id);
                    System.out.println("Элемент с id " + id + " успешно удален.");
                } catch (NumberFormatException e) {
                    System.out.println("Неверный формат id. Попробуйте еще раз.");
                }
                break;
            case "clear":
                clear();
                System.out.println("Коллекция успешно очищена.");
                break;
            case "save":
                saveToFile();
                System.out.println("Коллекция успешно сохранена в файл.");
                break;
            case "execute_script":
                if (commandParts.length < 2) {
                    System.out.println("Не указано имя файла скрипта.");
                    break;
                }
                executeScript(commandParts[1]);
                break;
            case "add_if_max":
                LabWork labWork = readLabWorkFromConsole(null, scanner);
                addIfMax(labWork);
                break;
            case "remove_greater":
                LabWork labWorkGreater = readLabWorkFromConsole(null, scanner);
                removeGreater(labWorkGreater);
                break;
            case "history":
                history();
                break;
            case "remove_any_by_difficulty":
                if (commandParts.length < 2) {
                    System.out.println("Не указана сложность для удаления.");
                    break;
                }
                try {
                    Difficulty difficulty = Difficulty.valueOf(commandParts[1]);
                    removeAnyByDifficulty(difficulty);
                } catch (IllegalArgumentException e) {
                    System.out.println("Неверная сложность. Попробуйте еще раз.");
                }
                break;
            case "print_unique_difficulty":
                printUniqueDifficulty();
                break;
            case "print_field_ascending_discipline":
                printFieldAscendingDiscipline();
                break;
            case "exit":
                System.out.println("Программа завершена.");
                this.exit = true;
                break;
            default:
                System.out.println("Неизвестная команда. Введите 'help' для получения списка доступных команд.");
                break;
        }
    }

    public LabWork readLabWorkFromConsole(LabWork existingLabWork, Scanner scanner) {

        String name = null;
        while (name == null || name.isEmpty()) {
            System.out.print("Введите имя: ");
            name = scanner.nextLine().trim();
            if (name.isEmpty()) {
                System.out.println("Имя не может быть пустым. Попробуйте еще раз.");
            }
        }

        Coordinates coordinates = null;
        while (coordinates == null) {
            try {
                System.out.print("Введите координату x (максимальное значение 337): ");
                long x = Long.parseLong(scanner.nextLine().trim());
                System.out.print("Введите координату y: ");
                float y = Float.parseFloat(scanner.nextLine().trim());
                coordinates = new Coordinates(x, y);
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод. Попробуйте еще раз.");
            }
        }

        int minimalPoint = -1;
        while (minimalPoint <= 0) {
            try {
                System.out.print("Введите минимальный балл (больше 0): ");
                minimalPoint = Integer.parseInt(scanner.nextLine().trim());
                if (minimalPoint <= 0) {
                    System.out.println("Минимальный балл должен быть больше 0. Попробуйте еще раз.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод. Попробуйте еще раз.");
            }
        }

        Double averagePoint = null;
        while (averagePoint == null || averagePoint <= 0) {
            try {
                System.out.print("Введите средний балл (больше 0): ");
                averagePoint = Double.parseDouble(scanner.nextLine().trim());
                if (averagePoint <= 0) {
                    System.out.println("Средний балл должен быть больше 0. Попробуйте еще раз.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод. Попробуйте еще раз.");
            }
        }

        Difficulty difficulty = null;
        while (difficulty == null) {
            System.out.print("Введите сложность (VERY_EASY, NORMAL, VERY_HARD, INSANE, HOPELESS): ");
            String difficultyInput = scanner.nextLine().trim();
            if (difficultyInput.isBlank()) {
                // Значение может быть null
                break;
            }
            try {
                difficulty = Difficulty.valueOf(difficultyInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Неверная сложность. Попробуйте еще раз.");
            }
        }

        Discipline discipline = null;
        while (discipline == null) {
            try {
                System.out.print("Введите название дисциплины: ");
                String disciplineName = scanner.nextLine().trim();
                if (disciplineName.isEmpty()) {
                    System.out.println("Название дисциплины не может быть пустым. Попробуйте еще раз.");
                    continue;
                }
                System.out.print("Введите количество часов практики: ");
                Long practiceHours = Long.parseLong(scanner.nextLine().trim());
                discipline = new Discipline(disciplineName, practiceHours);
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод. Попробуйте еще раз.");
            }
        }

        if (existingLabWork != null) {
            return new LabWork(existingLabWork.getId(), name, coordinates, existingLabWork.getCreationDate(),
                    minimalPoint, averagePoint, difficulty, discipline);
        }

        return new LabWork(name, coordinates, minimalPoint, averagePoint, difficulty, discipline);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Не заданно имя файла");
            return;
        }

        Application app = new Application(args[0]);
        Scanner scanner = new Scanner(System.in);

        while (!app.exit) {
            System.out.print("Введите команду: ");
            String command = scanner.nextLine().trim();
            app.executeCommand(command, scanner);
        }
        scanner.close();
    }
}
