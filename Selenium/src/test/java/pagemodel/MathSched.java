package pagemodel;/*import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;
/*import ru.abcconsulting.math.api.ViolationsCheckingAlgorithmDataInput;
import ru.abcconsulting.math.api.ViolationsCheckingAlgorithmOptions;
import ru.abcconsulting.math.api.data.*;
import ru.abcconsulting.math.impl.roster.ViolationsCheckingAlgorithmImpl;
import ru.abcconsulting.math.io.files.EmployeeFileIO;
import ru.abcconsulting.math.io.files.FteFileIO;
import ru.abcconsulting.math.io.files.RosterFileIO;
import ru.abcconsulting.math.io.files.WorkingScheduleFileIO;*/

public class MathSched {
/*
    //пункт 3, первая часть
   /* @Test
    public  void omShiftsFistHour() throws Exception {

        String[][] l = CsvLoader.csvShifts();

        //сумма по строчке
        int sum = 0;
        //строчка
        int i = 1;
        //колонка
        int j = 1;
        //счетчик количества
        int number = 0;

        for (;l.length>i;i++) {

            String s= "T10:00:00";

            if ((l[i][0]).contains(s)) {

                for (; j <= (l[0].length - 1); j++) {

                    sum = Integer.parseInt(l[i][j]) + sum;

                }

                if ((j==l[0].length)&&(sum<=1)) {

                    number++;
                    System.out.println("Номер строчки с ошибкой: "+(i+1));

                }

                //возвращаю параметры после цикла
                j=1;
                sum=0;

            }

        }

        System.out.println("Количество дней с ошибкой: "+number);

    }

    //каждый рабочий час есть не меньше 1ого сотрудника
    //
    @Test
    public void omShiftsCritical() throws Exception {

        String[][] l = CsvLoader.csvShifts();

        //сумма по строчке
        int sum = 0;
        //строчка
        int i = 1;
        //колонка
        int j = 1;
        //счетчик количества
        int number = 0;

        for (;l.length>i;i++) {

            String s= "T";

            if ((l[i][0]).contains(s)) {

                for (; j <= (l[0].length - 1); j++) {

                    sum = Integer.parseInt(l[i][j]) + sum;

                }

                if ((j==l[0].length)&&(sum<1)) {

                    number++;
                    System.out.println("Номер строчки с ошибкой: "+(i+1));

                }

                //возвращаю параметры после цикла
                j=1;
                sum=0;

            }

        }

        System.out.println("Количество дней с ошибкой: "+number);

    }

    @Test
    public void rostertest() throws Exception {

        //установка даты начала и окончания
        LocalDate startDate = LocalDate.of(2018, 1, 1);;
        LocalDate endDate = LocalDate.of(2018,1,31);
        DateInterval dateInterval = new DateInterval(startDate, endDate);

        //собираю сотрудников
        List<Employee> collectionEmp = EmployeeFileIO.read(Paths.get("datainput//1//employees"));

        //собираю смены
        List<SHIFT_REQUEST> shifts = RosterFileIO.read(Paths.get("datainput//1//shifts.csv"), collectionEmp);

        //собираю рабочее расписание
        WorkingSchedule workingSchedule = WorkingScheduleFileIO.read(Paths.get("datainput//1//schedule.csv"));

        //настройка алгоритма для расчета
        ViolationsCheckingAlgorithmDataInput dataInput = new ViolationsCheckingAlgorithmDataInput(shifts, workingSchedule);
        ViolationsCheckingAlgorithmOptions options = new ViolationsCheckingAlgorithmOptions(dateInterval);

        //наполняю данными
        dataInput.setEmployees(collectionEmp);
        dataInput.setFteList(FteFileIO.read(Paths.get("datainput//1//fte.csv")));

        //список ограничений
        List<ConstraintViolation> cvList = new ViolationsCheckingAlgorithmImpl().check(dataInput, options);
        //
        String csvFile = "datainput//Some.csv";


        FileWriter writer = new FileWriter(new File("datainput//Some2.csv"), false);
        *//*for (int i = 0;i<cvList.size();i++) {
                writer.write(cvList.get(i).toString()+"\n");
        }*//*

        writer.write("Айди точки");
        writer.close();

        Map<ConstraintViolationType, Long> s = cvList.stream()
                .collect(Collectors.groupingBy(ConstraintViolation::getType, Collectors.counting()));
        Writer writerRR = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(new File("datainput//Some3.csv")), StandardCharsets.UTF_8));
        s.forEach((key, value) -> {
            try {

                writerRR.write(key + "\t" + value + System.lineSeparator());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writerRR.flush();
        writerRR.close();



    }
    @Test
    public void prop() throws Exception {
        //так лучше не делать

        FileWriter wThird = new FileWriter(new File("datainput//Some.csv"), true);
        List<String> lines = new ArrayList<String>();

        File fOne = new File("datainput//Some.csv");
        File fTwo = new File("datainput//Some2.csv");
        File fThree = new File("datainput//Some3.csv");

        FileInputStream inStream = new FileInputStream(fOne);
        FileInputStream outStreamOne = new FileInputStream(fTwo);
        FileInputStream outStreamTwo = new FileInputStream(fThree);

        wThird.flush();
        wThird.close();
    }



    @Test
    public void downloadConsrains() throws Exception {

        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, 90, 500);
        CookieRW.writeCookie(driver, "http://104.196.10.86:9000");
        int[] orgunitid = {218};
        int i = 0;
        Actions act  = new Actions(driver);
        while (orgunitid.length>i) {

            driver.navigate().to("http://104.196.10.86:8080/api/v1/org-units/"+orgunitid[i]+"/constraint-violations?from=2017-01-01&to=2017-12-31");
            Thread.sleep(5000);
            String s = driver.findElement(By.xpath("//pre")).getText();
            FileWriter writer = new FileWriter(new File("datainput/Some.txt"), false);
            writer.write(s);
            writer.flush();
            i++;
        }
    }*/
}
