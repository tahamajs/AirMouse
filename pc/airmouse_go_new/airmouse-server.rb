class AirmouseServer < Formula
  desc "Air Mouse Pro Server - Smartphone-based remote control host"
  homepage "https://github.com/tahamajs/airmouse-server"
  url "file:///Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code/pc/airmouse_go_new"
  version "4.2.0"

  depends_on "go" => :build

  def install
    # Build the binary
    system "go", "build", "-o", bin/"airmouse-server", "./cmd/airmouse-server"
  end

  service do
    run [opt_bin/"airmouse-server", "-headless"]
    keep_alive true
    working_dir var/"airmouse-server"
    log_path var/"log/airmouse-server.log"
    error_log_path var/"log/airmouse-server.err.log"
  end

  def post_install
    (var/"airmouse-server").mkpath
  end

  test do
    assert_match "Air Mouse Pro Server", shell_output("#{bin}/airmouse-server -version")
  end
end
