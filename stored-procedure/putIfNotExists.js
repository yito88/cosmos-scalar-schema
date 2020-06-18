function putIfNotExists(itemToCreate, query) {
  var container = getContext().getCollection();

  var isAccepted = container.queryDocuments(
      container.getSelfLink(),
      query,
      function (err, items, options) {
          if (err) throw err;

          if (!items || !items.length) {
            var accepted = container.createDocument(container.getSelfLink(), itemToCreate);
            if (!accepted) throw new Error("Failed to insert");
          } else {
            throw new Error("Already exists");
          }
      });
  if (!isAccepted) throw new Error("The query was not accepted by the server.");
}
