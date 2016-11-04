xquery version "1.0-ml";

module namespace env = "http://marklogic.com/rest-api/transform/envelope";

declare namespace es = "http://marklogic.com/entity-services";

declare function env:transform(
  $context as map:map,
  $params as map:map,
  $content as document-node())
as document-node() {

   document {
     element es:envelope {
       element es:instance {
         element es:info {
           element es:title { "TITLE" },
           element es:version { "1.0" }
         },
         $content/element(),
         for $key in map:keys($params)
         return element { $key } { map:get($params, $key) }
       },
       element es:attachments {

       }
     }
   }
};