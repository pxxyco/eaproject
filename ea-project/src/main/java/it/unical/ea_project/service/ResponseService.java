package it.unical.ea_project.service;

import it.unical.ea_project.dto.ResponseDTO;



public interface ResponseService {
    ResponseDTO create(Long reviewId, String comment, Long organizerId);
    ResponseDTO update(Long id, String comment, Long organizerId);
    void delete(Long id, Long organizerId);
}