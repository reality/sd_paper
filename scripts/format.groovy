@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

def DATA_DIR = '../data_ws/'

def purchasedDoids = new File('./data/mapped_purchase_query_doid.csv').text.split('\n').collect { it.tokenize(',').last() }

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
            ps = record[5]
              .tokenize(';').collect { 
                def m = (it =~ /hp_\d+/)
                m.size() > 0 ? m[0].toUpperCase() : null 
              }
              .findAll { it != null }
              .collect { it.replace('_', ':') }
          }
          if(record[4] != '') {
            ps2 = record[4]
              .tokenize(';')
              .collect { 
                def m = (it =~ /doid_\d+/)
                m.size() > 0 ? m[0].toUpperCase() : null 
              }
              .findAll { it != null }
              .collect { it.replace('_', ':') }
              .findAll { purchasedDoids.contains(it) }
          }

          if(ps2.size() > 0) {
            println counter + '\t' + ps.join(';') + '\t' + ps2.join(';')
          }
        } catch(e) { 
          println e.toString() 
        }
      }
    }
  } 
}
