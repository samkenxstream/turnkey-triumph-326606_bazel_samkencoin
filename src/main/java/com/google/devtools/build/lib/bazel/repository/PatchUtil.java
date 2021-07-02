import com.google.common.annotations.VisibleForTesting;
import java.nio.charset.StandardCharsets;
   * Sometimes the line number in patch file is not completely correct, but we might still be able
   * to find a content match with an offset.
  private static class OffsetPatch {

    public static List<String> applyTo(Patch<String> patch, List<String> target)
        throws PatchFailedException {
      List<AbstractDelta<String>> deltas = patch.getDeltas();
      List<String> result = new ArrayList<>(target);
      for (AbstractDelta<String> item : Lists.reverse(deltas)) {
        AbstractDelta<String> delta = item;
        applyTo(delta, result);
      }

      return result;
    /**
     * This function first tries to apply the Delta without any offset, if that fails, then it tries
     * to apply the Delta with an offset, starting from 1, up to the total lines in the original
     * content. For every offset, we try both forwards and backwards.
     */
    private static void applyTo(AbstractDelta<String> delta, List<String> result)
        throws PatchFailedException {
      PatchFailedException e = applyDelta(delta, result);
      if (e == null) {
        return;
      }

      Chunk<String> original = delta.getSource();
      Chunk<String> revised = delta.getTarget();
      int[] direction = {1, -1};
      int maxOffset = result.size();
      for (int i = 1; i < maxOffset; i++) {
        for (int j = 0; j < 2; j++) {
          int offset = i * direction[j];
          if (offset + original.getPosition() < 0 || offset + revised.getPosition() < 0) {
            continue;
          }
          Chunk<String> source = new Chunk<>(original.getPosition() + offset, original.getLines());
          Chunk<String> target = new Chunk<>(revised.getPosition() + offset, revised.getLines());
          AbstractDelta<String> newDelta = null;
          switch (delta.getType()) {
            case CHANGE:
              newDelta = new ChangeDelta<>(source, target);
              break;
            case INSERT:
              newDelta = new InsertDelta<>(source, target);
              break;
            case DELETE:
              newDelta = new DeleteDelta<>(source, target);
              break;
            case EQUAL:
          }
          PatchFailedException exception = null;
          if (newDelta != null) {
            exception = applyDelta(newDelta, result);
          }
          if (exception == null) {
            return;
          }
      throw e;
    }
    private static PatchFailedException applyDelta(
        AbstractDelta<String> delta, List<String> result) {
      try {
        delta.applyTo(result);
        return null;
      } catch (PatchFailedException e) {
        String msg =
            String.join(
                "\n",
                "**Original Position**: " + (delta.getSource().getPosition() + 1) + "\n",
                "**Original Content**:",
                String.join("\n", delta.getSource().getLines()) + "\n",
                "**Revised Content**:",
                String.join("\n", delta.getTarget().getLines()) + "\n");
        return new PatchFailedException(e.getMessage() + "\n" + msg);
      }
  @VisibleForTesting
  public static List<String> readFile(Path file) throws IOException {
    return Lists.newArrayList(FileSystemUtils.readLines(file, StandardCharsets.UTF_8));
    FileSystemUtils.writeLinesAs(file, StandardCharsets.UTF_8, content);
    List<String> oldContent;
      oldContent = new ArrayList<>();
    List<String> newContent = OffsetPatch.applyTo(patch, oldContent);
    List<String> patchFileLines = readFile(patchFile);
    // Adding an extra line to make sure last chunk also gets applied.
    patchFileLines.add("$");
    for (int i = 0; i < patchFileLines.size(); i++) {
      String line = patchFileLines.get(i);