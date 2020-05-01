module fr.umlv.jsonapi {
  requires com.fasterxml.jackson.core;

  exports fr.umlv.jsonapi;
  exports fr.umlv.jsonapi.builder;
  exports fr.umlv.jsonapi.bind;
  // fr.umlv.jsonapi.internal is not exported !
}