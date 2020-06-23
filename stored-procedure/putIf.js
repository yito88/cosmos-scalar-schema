function putIf(item, query) {

  function merge(source, update) {
    for (var column in update) {
      if (update[column].constructor == Object) {
        source[column] = merge(source[column], update[column]);
      } else {
        source[column] = update[column];
      }
    }

    return source;
  }

  var isAccepted = __.queryDocuments(
      __.getSelfLink(),
      query,
      function (err, items, options) {
          if (err) throw err;

          if (!items || !items.length) {
            // The specified record does not exist
            getContext().getResponse().setBody(false);
            return;
          } else {
            if (items.length != 1) throw new Error("Unable to put for multiple records.");
            var accepted = __.replaceDocument(items[0]._self, merge(items[0], item));
            if (!accepted) throw new Error("Failed to update.");
            getContext().getResponse().setBody(true);
          }
      });
  if (!isAccepted) throw new Error("The query was not accepted by the server.");
}
