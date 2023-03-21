klarigi --data data/format_class_for_ic/dises.tsv --ontology data/doid.owl --save-ic data/get_ic/disease_ic.tsv --verbose --scores-only
klarigi --data data/format_class_for_ic/phens.tsv --ontology data/hp.owl --save-ic data/get_ic/phens_ic.tsv --verbose --scores-only --reroot 'http://purl.obolibrary.org/obo/HP_0000001'
