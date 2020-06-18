function deleteIf(query) {
  var container = getContext().getCollection();

  var isAccepted = container.queryDocuments(
      container.getSelfLink(),
      query,
      function (err, items, options) {
          if (err) throw err;

          if (!items || !items.length) {
            throw new Error("The specified record does not exist.");
          } else {
            if (items.length != 1) throw new Error("Unable to put for multiple records.");
            var accepted = container.deleteDocument(items[0]._self);
            if (!accepted) throw new Error("Failed to update.");
          }
      });
  if (!isAccepted) throw new Error("The query was not accepted by the server.");
}
