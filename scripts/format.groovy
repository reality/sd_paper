@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

def DATA_DIR = '../data_ws/'

def counter = 0
new File(DATA_DIR).eachFile { fi ->
  if(fi.getName() =~ /csv$/) { 
    fi.withReader { reader ->
      CSVParser csv = new CSVParser(reader, DEFAULT)
      for(record in csv.iterator()) {
        counter++
        try {
          def ps = []
          def ps2 = []
          if(record[5] != '') {
            ps = record[5].tokenize(';').collect { 
              def m = (it =~ /hp_\d+/)
              m.size() > 0 ? m[0].toUpperCase() : null 
            }.findAll { it != null }
          }
          if(record[4] != '') {
            ps2 = record[4].tokenize(';').collect { 
              def m = (it =~ /doid_\d+/)
              m.size() > 0 ? m[0].toUpperCase() : null 
            }.findAll { it != null }
          }

          println counter + '\t' + ps.join(';').replaceAll('_',':') + '\t' + ps2.join(';').replaceAll('_',':')
        } catch(e) { 
          println e.toString() 
        }
      }
    }
  } 
}
