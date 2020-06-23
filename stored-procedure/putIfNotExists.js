function putIfNotExists(item, query) {
  var isAccepted = __.queryDocuments(
      __.getSelfLink(),
      query,
      function (err, items, options) {
          if (err) throw err;

          if (!items || !items.length) {
            var accepted = __.createDocument(__.getSelfLink(), item);
            if (!accepted) throw new Error("Failed to insert");
            getContext().getResponse().setBody(true);
          } else {
            // The specified record already exists
            getContext().getResponse().setBody(false);
          }
      });
  if (!isAccepted) throw new Error("The query was not accepted by the server.");
}
