def scores = []
new File('data/run_permutations/').eachFile { f ->
  if(f.getName() =~ /association/) {
    f.splitEachLine('\t') {
      if(it[0] == 'DOID:2723' && it[1] == 'HP:0012372') {
        scores += it[2].tokenize(',')
      }
    }  
  }
}

new File('data/norm.tsv').text = scores.join('\n')
