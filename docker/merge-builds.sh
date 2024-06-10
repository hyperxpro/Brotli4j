OUT="$PWD"
rootPaths=($(find ./builds/*/natives/ -type d -name "target" | grep -oiP '(.*)\/([^-]*-[^-]*)-Build-JDK([0-9]*)(?=\/natives\/\2\/target)'))
paths=($(find ./builds/*/natives/ -type d -name "target" | grep -oiP '(?:(.*)\/([^-]*-[^-]*)-Build-JDK([0-9]*)\/)natives\/\2\/target'))
for i in "${!paths[@]}"; do
  rootPath="${rootPaths[$i]}"
  rootPathLen="${#rootPath}"
  path="${paths[$i]}"

  cd "$rootPath"
  path=.${path:rootPathLen:256}

  cp -r --parent "$path" "$OUT"
  cd "$OUT"
done