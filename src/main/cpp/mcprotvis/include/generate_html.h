#pragma once
#include <string>
#include <vector>
#include "pcap_reader.hpp"
#include "packet_analysis.hpp"

bool generateHtml(const std::string& path,
                  const std::vector<std::pair<PacketRecord, PacketInfo>>& packets,
                  int totalScanned, int serverPkts, int clientPkts);
