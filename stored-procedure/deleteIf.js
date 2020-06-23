function deleteIf(query) {

  var isAccepted = __.queryDocuments(
      __.getSelfLink(),
      query,
      function (err, items, options) {
          if (err) throw err;

          if (!items || !items.length) {
            // The specified record does not exist
            getContext().getResponse().setBody(false);
          } else {
            if (items.length != 1) throw new Error("Unable to delete for multiple records.");
            var accepted = __.deleteDocument(items[0]._self);
            if (!accepted) throw new Error("Failed to delete");
            getContext().getResponse().setBody(true);
          }
      });
  if (!isAccepted) throw new Error("The query was not accepted by the server.");
}
