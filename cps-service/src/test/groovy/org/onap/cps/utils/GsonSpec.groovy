package org.onap.cps.utils

import com.google.gson.stream.JsonReader
import org.onap.cps.TestUtils
import spock.lang.Specification


class GsonSpec extends Specification{

    def 'Iterate over JSON data with gson JsonReader'(){
        given: 'json data with two objects and JSON reader'
            def jsonData = TestUtils.getResourceFileContent('multiple-object-data.json')
            def objectUnderTest = new JsonReader(new StringReader(jsonData));
        when: 'data is iterated over with JsonReader methods'
            iterateWithJsonReader(objectUnderTest)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def iterateWithJsonReader(JsonReader jsonReader){
        switch(jsonReader.peek()) {
            case "STRING":
                print(jsonReader.nextString() + " ")
                break;
            case "BEGIN_OBJECT":
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    print(jsonReader.nextName() + " ")
                    iterateWithJsonReader(jsonReader)
                }
                jsonReader.endObject();
                println("")
                break;
            default:
                break;
        }
    }


}
