import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

public class Main {
    public static void main(String[] args) {
        Person person = new Person();
        person.setId(1);
        person.setName("Muhammed Emin");
        person.setSurname("YILMAZ");

        Gson gsonBuilder = new GsonBuilder().create();
        System.out.println(gsonBuilder.toJson(person));

        Gson gson = new Gson();
        JsonParser jsonParser = new JsonParser();
        //jsonParser.parse("{\"id\":1,\"name\":\"Muhammed Emin\",\"surname\":\"YILMAZ\"}");
        Person readPerson = gson.fromJson(gsonBuilder.toJson(person), Person.class);
        System.out.println(readPerson.getId() + " " + readPerson.getName() + " " + readPerson.getSurname());

    }
}
